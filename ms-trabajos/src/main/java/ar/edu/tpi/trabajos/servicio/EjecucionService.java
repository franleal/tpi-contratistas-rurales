package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.AccesoRecursoException;
import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient.HoraClima;
import ar.edu.tpi.trabajos.dominio.Asignacion;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.FotoClima;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.RegistroEjecucion;
import ar.edu.tpi.trabajos.dominio.UmbralClimatico;
import ar.edu.tpi.trabajos.repositorio.AsignacionRepository;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.RegistroEjecucionRepository;
import ar.edu.tpi.trabajos.web.dto.EjecucionRequest;
import ar.edu.tpi.trabajos.web.dto.OrdenDetalleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** F-16: registro de la ejecución por el contratista asignado (RN-15). */
@Service
public class EjecucionService {

    private static final Logger log = LoggerFactory.getLogger(EjecucionService.class);

    private final OrdenService ordenService;
    private final OrdenTrabajoRepository ordenes;
    private final AsignacionRepository asignaciones;
    private final RegistroEjecucionRepository ejecuciones;
    private final ConsultaOrdenService consultas;
    private final TransactionTemplate tx;
    private final Clock reloj;
    private final UmbralService umbrales;
    private final OpenMeteoClient meteo;
    private final EvaluadorClima evaluador;

    public EjecucionService(OrdenService ordenService, OrdenTrabajoRepository ordenes,
                            AsignacionRepository asignaciones, RegistroEjecucionRepository ejecuciones,
                            ConsultaOrdenService consultas, TransactionTemplate tx, Clock reloj,
                            UmbralService umbrales, OpenMeteoClient meteo, EvaluadorClima evaluador) {
        this.umbrales = umbrales;
        this.meteo = meteo;
        this.evaluador = evaluador;
        this.ordenService = ordenService;
        this.ordenes = ordenes;
        this.asignaciones = asignaciones;
        this.ejecuciones = ejecuciones;
        this.consultas = consultas;
        this.tx = tx;
        this.reloj = reloj;
    }

    public OrdenDetalleResponse registrar(Long ordenId, EjecucionRequest req) {
        OrdenTrabajo orden = ordenService.buscar(ordenId);
        orden.exigirEstado(EstadoOrden.ASIGNADA, "registrar la ejecución");
        Asignacion asignacion = asignaciones.findByOrdenId(ordenId)
                .orElseThrow(() -> new ConflictoEstadoException("La orden " + ordenId + " no tiene asignación"));
        if (!asignacion.getPostulacion().getContratistaSub().equals(CurrentUser.sub())) {
            throw new AccesoRecursoException("Solo el contratista asignado puede registrar la ejecución (RN-15)");
        }
        validar(orden, req);

        RegistroEjecucion r = new RegistroEjecucion();
        r.setInicio(req.inicio());
        r.setFin(req.fin());
        r.setHectareas(req.hectareas().setScale(2, RoundingMode.HALF_UP));
        completarClima(orden, r);

        OrdenTrabajo ejecutada = tx.execute(estado -> guardar(ordenId, r));
        log.info("Ejecución registrada en la orden {} (clima disponible: {}, fuera de condiciones: {})", ordenId,
                r.isClimaDisponible(), r.isFueraDeCondiciones());
        return consultas.detalleDe(ejecutada);
    }

    /**
     * RN-16: guarda el clima real de cada hora trabajada y marca si alguna estuvo fuera de umbral.
     * Si Open-Meteo falla (o le faltan horas), la ejecución se registra igual sin foto climática.
     */
    void completarClima(OrdenTrabajo orden, RegistroEjecucion r) {
        UmbralClimatico u = umbrales.vigentes();
        try {
            Map<LocalDateTime, HoraClima> clima = meteo.horas(orden.getLoteLat().doubleValue(),
                    orden.getLoteLon().doubleValue(), r.getInicio().toLocalDate(), r.getFin().toLocalDate());
            List<FotoClima> fotos = new ArrayList<>();
            for (LocalDateTime h : EvaluadorClima.horasDeAplicacion(r.getInicio(), r.getFin())) {
                HoraClima c = clima.get(h);
                if (c == null) {
                    throw new DependenciaNoDisponibleException("Open-Meteo no devolvió el clima de la hora " + h);
                }
                Optional<String> falla = evaluador.fallaDeAplicacion(c, u);
                FotoClima f = new FotoClima();
                f.setHora(h);
                f.setVientoKmh(c.vientoKmh());
                f.setRafagaKmh(c.rafagaKmh());
                f.setPrecipitacionMm(c.precipitacionMm());
                f.setTemperaturaC(c.temperaturaC());
                f.setHumedadPct(c.humedadPct());
                f.setApta(falla.isEmpty());
                f.setMotivo(falla.orElse(null));
                fotos.add(f);
            }
            fotos.forEach(r::agregarFoto);
            r.setClimaDisponible(true);
            r.setFueraDeCondiciones(fotos.stream().anyMatch(f -> !f.isApta()));
        } catch (DependenciaNoDisponibleException e) {
            log.warn("Foto climática no disponible para la orden {}: {}", orden.getId(), e.getMessage());
            r.getFotos().clear();
            r.setClimaDisponible(false);
            r.setFueraDeCondiciones(false);
        }
    }

    /**
     * RN-15: fin posterior al inicio y no futuro; hectáreas mayores que 0, con a lo sumo dos decimales
     * (se guardan con escala 2: 0.001 quedaría en 0.00) y hasta la superficie del lote.
     */
    private void validar(OrdenTrabajo orden, EjecucionRequest req) {
        if (!req.fin().isAfter(req.inicio())) {
            throw new ReglaNegocioException("El fin de la ejecución debe ser posterior al inicio (RN-15)");
        }
        if (req.fin().isAfter(LocalDateTime.now(reloj))) {
            throw new ReglaNegocioException("El fin de la ejecución no puede estar en el futuro (RN-15)");
        }
        if (req.hectareas().stripTrailingZeros().scale() > 2) {
            throw new ReglaNegocioException("Las hectáreas aplicadas admiten a lo sumo dos decimales (RN-15)");
        }
        if (req.hectareas().signum() <= 0 || req.hectareas().compareTo(orden.getSuperficieHa()) > 0) {
            throw new ReglaNegocioException("Las hectáreas aplicadas deben ser mayores que 0 y no superar la "
                    + "superficie del lote (" + orden.getSuperficieHa() + " ha) (RN-15)");
        }
    }

    private OrdenTrabajo guardar(Long ordenId, RegistroEjecucion r) {
        OrdenTrabajo orden = ordenes.bloquear(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden " + ordenId));
        orden.exigirEstado(EstadoOrden.ASIGNADA, "registrar la ejecución");
        r.setOrden(orden);
        ejecuciones.save(r);
        orden.avanzarA(EstadoOrden.EJECUTADA);
        return orden;
    }
}
