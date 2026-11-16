package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient.HoraClima;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.UmbralClimatico;
import ar.edu.tpi.trabajos.web.dto.VentanaResponse;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * F-12: tramos de horas consecutivas que cumplen RN-09 dentro del horizonte y hasta la fecha
 * límite. Una hora es apta si cumple los umbrales y no llueve en ella ni en las N siguientes.
 */
@Service
public class VentanaService {

    private final OrdenService ordenes;
    private final UmbralService umbrales;
    private final OpenMeteoClient meteo;
    private final EvaluadorClima evaluador;
    private final Clock reloj;

    public VentanaService(OrdenService ordenes, UmbralService umbrales, OpenMeteoClient meteo,
                          EvaluadorClima evaluador, Clock reloj) {
        this.ordenes = ordenes;
        this.umbrales = umbrales;
        this.meteo = meteo;
        this.evaluador = evaluador;
        this.reloj = reloj;
    }

    public List<VentanaResponse> ventanas(Long ordenId) {
        OrdenTrabajo o = CurrentUser.tieneRol("PRODUCTOR") ? ordenes.propia(ordenId) : ordenes.buscar(ordenId);
        o.exigirEstado(EstadoOrden.ABIERTA, "consultar ventanas");
        UmbralClimatico u = umbrales.vigentes();

        LocalDateTime desde = LocalDateTime.now(reloj).truncatedTo(ChronoUnit.HOURS).plusHours(1);
        LocalDateTime hasta = desde.plusDays(u.getHorizonteDias());
        LocalDateTime finLimite = o.getFechaLimite().plusDays(1).atStartOfDay();
        if (finLimite.isBefore(hasta)) {
            hasta = finLimite;
        }
        if (!desde.isBefore(hasta)) {
            return List.of();
        }
        Map<LocalDateTime, HoraClima> pronostico = meteo.horas(o.getLoteLat().doubleValue(),
                o.getLoteLon().doubleValue(), desde.toLocalDate(),
                hasta.plusHours(u.getHorasSinLluviaDespues()).toLocalDate());
        return tramos(pronostico, desde, hasta, u);
    }

    List<VentanaResponse> tramos(Map<LocalDateTime, HoraClima> p, LocalDateTime desde, LocalDateTime hasta,
                                 UmbralClimatico u) {
        List<VentanaResponse> tramos = new ArrayList<>();
        LocalDateTime inicioTramo = null;
        for (LocalDateTime h = desde; h.isBefore(hasta); h = h.plusHours(1)) {
            boolean apta = apta(p, h, u);
            if (apta && inicioTramo == null) {
                inicioTramo = h;
            } else if (!apta && inicioTramo != null) {
                tramos.add(tramo(inicioTramo, h));
                inicioTramo = null;
            }
        }
        if (inicioTramo != null) {
            tramos.add(tramo(inicioTramo, hasta));
        }
        return tramos;
    }

    private boolean apta(Map<LocalDateTime, HoraClima> p, LocalDateTime h, UmbralClimatico u) {
        HoraClima c = p.get(h);
        if (c == null || evaluador.fallaDeAplicacion(c, u).isPresent()) {
            return false;
        }
        for (int i = 1; i <= u.getHorasSinLluviaDespues(); i++) {
            HoraClima siguiente = p.get(h.plusHours(i));
            if (siguiente == null || siguiente.precipitacionMm() > 0) {
                return false;
            }
        }
        return true;
    }

    private static VentanaResponse tramo(LocalDateTime desde, LocalDateTime hasta) {
        return new VentanaResponse(desde, hasta, (int) ChronoUnit.HOURS.between(desde, hasta));
    }
}
