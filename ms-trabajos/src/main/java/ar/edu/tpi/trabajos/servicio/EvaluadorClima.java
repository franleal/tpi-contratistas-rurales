package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient.HoraClima;
import ar.edu.tpi.trabajos.dominio.UmbralClimatico;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * RN-09: todas las horas de la aplicación cumplen viento, ráfaga, temperatura y humedad, y no
 * hay lluvia desde el inicio hasta N horas después del fin. Las horas se toman completas: la
 * hora que contiene al inicio y todas las que se solapan con [inicio, fin).
 */
@Component
public class EvaluadorClima {

    /** Resultado: si no es apta, la primera hora que falla y el motivo. */
    public record Resultado(boolean apta, LocalDateTime primeraHoraQueFalla, String motivo) {
        static Resultado ok() {
            return new Resultado(true, null, null);
        }
    }

    public Resultado evaluar(Map<LocalDateTime, HoraClima> pronostico, LocalDateTime inicio, LocalDateTime fin,
                             UmbralClimatico u) {
        for (LocalDateTime h : horasDeAplicacion(inicio, fin)) {
            Optional<String> falla = fallaDeAplicacion(hora(pronostico, h), u);
            if (falla.isPresent()) {
                return new Resultado(false, h, "La hora " + h + " no es apta: " + falla.get() + " (RN-09)");
            }
        }
        for (LocalDateTime h : horasSinLluvia(inicio, fin, u.getHorasSinLluviaDespues())) {
            HoraClima c = hora(pronostico, h);
            if (c.precipitacionMm() > 0) {
                return new Resultado(false, h, "Se pronostica lluvia (" + c.precipitacionMm() + " mm) a las " + h
                        + ", dentro de las " + u.getHorasSinLluviaDespues() + " h posteriores al fin (RN-09)");
            }
        }
        return Resultado.ok();
    }

    /** Motivo por el que una hora incumple viento, ráfaga, temperatura, humedad o lluvia; vacío si cumple. */
    public Optional<String> fallaDeAplicacion(HoraClima c, UmbralClimatico u) {
        List<String> fallas = new ArrayList<>();
        if (c.vientoKmh() < u.getVientoMinKmh().doubleValue()) {
            fallas.add("viento " + c.vientoKmh() + " km/h menor al mínimo " + u.getVientoMinKmh());
        }
        if (c.vientoKmh() > u.getVientoMaxKmh().doubleValue()) {
            fallas.add("viento " + c.vientoKmh() + " km/h mayor al máximo " + u.getVientoMaxKmh());
        }
        if (c.rafagaKmh() > u.getRafagaMaxKmh().doubleValue()) {
            fallas.add("ráfaga " + c.rafagaKmh() + " km/h mayor a " + u.getRafagaMaxKmh());
        }
        if (c.temperaturaC() > u.getTemperaturaMaxC().doubleValue()) {
            fallas.add("temperatura " + c.temperaturaC() + " °C mayor a " + u.getTemperaturaMaxC());
        }
        if (c.humedadPct() < u.getHumedadMinPct().doubleValue()) {
            fallas.add("humedad " + c.humedadPct() + " % menor a " + u.getHumedadMinPct());
        }
        if (c.precipitacionMm() > 0) {
            fallas.add("lluvia " + c.precipitacionMm() + " mm");
        }
        return fallas.isEmpty() ? Optional.empty() : Optional.of(String.join(", ", fallas));
    }

    /** Horas completas que se solapan con [inicio, fin). */
    public static List<LocalDateTime> horasDeAplicacion(LocalDateTime inicio, LocalDateTime fin) {
        List<LocalDateTime> horas = new ArrayList<>();
        for (LocalDateTime h = inicio.truncatedTo(ChronoUnit.HOURS); h.isBefore(fin); h = h.plusHours(1)) {
            horas.add(h);
        }
        return horas;
    }

    /** Horas que no deben tener lluvia: las de la aplicación y las N siguientes al fin. */
    public static List<LocalDateTime> horasSinLluvia(LocalDateTime inicio, LocalDateTime fin, int n) {
        return horasDeAplicacion(inicio, fin.plusHours(n));
    }

    private static HoraClima hora(Map<LocalDateTime, HoraClima> pronostico, LocalDateTime h) {
        HoraClima c = pronostico.get(h);
        if (c == null) {
            throw new DependenciaNoDisponibleException("Open-Meteo no devolvió el pronóstico de la hora " + h);
        }
        return c;
    }
}
