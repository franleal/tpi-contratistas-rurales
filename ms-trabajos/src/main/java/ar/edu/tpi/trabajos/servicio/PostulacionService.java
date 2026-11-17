package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.AccesoRecursoException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Postulacion;
import ar.edu.tpi.trabajos.dominio.UmbralClimatico;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.PostulacionRepository;
import ar.edu.tpi.trabajos.servicio.CalculadoraPostulacion.Calculo;
import ar.edu.tpi.trabajos.web.dto.PostulacionRequest;
import ar.edu.tpi.trabajos.web.dto.PostulacionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * F-13. Las validaciones que consultan otros servicios se hacen fuera de la transacción; solo
 * se guarda si todas pasan. Cualquier dependencia caída responde 503 sin guardar (falla segura).
 */
@Service
public class PostulacionService {

    private static final Logger log = LoggerFactory.getLogger(PostulacionService.class);

    private final OrdenService ordenService;
    private final OrdenTrabajoRepository ordenes;
    private final PostulacionRepository postulaciones;
    private final UmbralService umbrales;
    private final ContratistasClient contratistas;
    private final OpenMeteoClient meteo;
    private final EvaluadorClima evaluador;
    private final CalculadoraPostulacion calculadora;
    private final TransactionTemplate tx;
    private final Clock reloj;

    public PostulacionService(OrdenService ordenService, OrdenTrabajoRepository ordenes,
                              PostulacionRepository postulaciones, UmbralService umbrales,
                              ContratistasClient contratistas, OpenMeteoClient meteo, EvaluadorClima evaluador,
                              CalculadoraPostulacion calculadora, TransactionTemplate tx, Clock reloj) {
        this.ordenService = ordenService;
        this.ordenes = ordenes;
        this.postulaciones = postulaciones;
        this.umbrales = umbrales;
        this.contratistas = contratistas;
        this.meteo = meteo;
        this.evaluador = evaluador;
        this.calculadora = calculadora;
        this.tx = tx;
        this.reloj = reloj;
    }

    public PostulacionResponse postularse(Long ordenId, PostulacionRequest req) {
        OrdenTrabajo orden = ordenService.buscar(ordenId);
        orden.exigirEstado(EstadoOrden.ABIERTA, "postularse");
        UmbralClimatico u = umbrales.vigentes();
        LocalDateTime ahora = LocalDateTime.now(reloj);

        // RN-10 (inicio)
        if (!req.inicio().isAfter(ahora)) {
            throw new ReglaNegocioException("El inicio " + req.inicio() + " debe ser posterior al momento actual (RN-10)");
        }

        // RN-02 a RN-04 en la fecha del trabajo; propiedad de la máquina
        ContratistasClient.Habilitacion h = contratistas.habilitacion(req.maquinaId(), req.aplicadorId(),
                req.inicio().toLocalDate());
        if (!CurrentUser.sub().equals(h.contratistaSub())) {
            throw new AccesoRecursoException("La máquina " + req.maquinaId() + " no pertenece al contratista");
        }
        exigirHabilitado(h);

        // RN-11 y RN-12
        Calculo c = calculadora.calcular(orden.getSuperficieHa(), h.rendimientoHaH(),
                orden.getLoteLat().doubleValue(), orden.getLoteLon().doubleValue(),
                h.baseLat().doubleValue(), h.baseLon().doubleValue(), req.inicio());

        // RN-10 (fin): dentro del horizonte del pronóstico y no después de la fecha límite
        LocalDateTime finHorizonte = ahora.plusDays(u.getHorizonteDias());
        if (c.fin().isAfter(finHorizonte)) {
            throw new ReglaNegocioException("El fin " + c.fin() + " queda fuera del horizonte del pronóstico ("
                    + u.getHorizonteDias() + " días, hasta " + finHorizonte.withNano(0) + ") (RN-10)");
        }
        if (c.fin().toLocalDate().isAfter(orden.getFechaLimite())) {
            throw new ReglaNegocioException("El fin " + c.fin() + " es posterior a la fecha límite de la orden "
                    + orden.getFechaLimite() + " (RN-10)");
        }
        // si el trabajo termina otro día, las habilitaciones también deben cubrir ese día
        if (!c.fin().toLocalDate().equals(req.inicio().toLocalDate())) {
            exigirHabilitado(contratistas.habilitacion(req.maquinaId(), req.aplicadorId(), c.fin().toLocalDate()));
        }

        // RN-13: intervalo ocupado (con traslado) dentro de un bloque y sin otra reserva
        ContratistasClient.Disponibilidad d = contratistas.disponibilidad(req.maquinaId(), c.ocupadoDesde(), c.fin());
        if (!d.disponible()) {
            throw new ReglaNegocioException("La máquina " + req.maquinaId() + " no está disponible entre "
                    + c.ocupadoDesde() + " y " + c.fin() + ": " + d.motivo());
        }

        // RN-09: pronóstico de todas las horas + N horas sin lluvia
        var pronostico = meteo.horas(orden.getLoteLat().doubleValue(), orden.getLoteLon().doubleValue(),
                c.inicio().toLocalDate(), c.fin().plusHours(u.getHorasSinLluviaDespues()).toLocalDate());
        EvaluadorClima.Resultado clima = evaluador.evaluar(pronostico, c.inicio(), c.fin(), u);
        if (!clima.apta()) {
            throw new ReglaNegocioException(clima.motivo());
        }

        Postulacion p = tx.execute(estado -> guardar(ordenId, req, c));
        log.info("Postulación {} VALIDA en la orden {}: máquina {} de {} a {}", p.getId(), ordenId, req.maquinaId(),
                c.inicio(), c.fin());
        return PostulacionResponse.de(p);
    }

    private Postulacion guardar(Long ordenId, PostulacionRequest req, Calculo c) {
        OrdenTrabajo orden = ordenes.bloquear(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden " + ordenId));
        orden.exigirEstado(EstadoOrden.ABIERTA, "postularse"); // pudo asignarse mientras se validaba
        Postulacion p = new Postulacion();
        p.setOrden(orden);
        p.setContratistaSub(CurrentUser.sub());
        p.setMaquinaId(req.maquinaId());
        p.setAplicadorId(req.aplicadorId());
        p.setInicio(c.inicio());
        p.setFin(c.fin());
        p.setOcupadoDesde(c.ocupadoDesde());
        p.setHorasAplicacion(c.horasAplicacion());
        p.setTrasladoH(c.trasladoH());
        p.setPrecioHa(req.precioHa());
        return postulaciones.save(p);
    }

    static void exigirHabilitado(ContratistasClient.Habilitacion h) {
        if (!h.habilitado()) {
            throw new ReglaNegocioException(h.motivo());
        }
    }
}
