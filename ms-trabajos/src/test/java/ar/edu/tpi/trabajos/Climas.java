package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.cliente.OpenMeteoClient.HoraClima;
import ar.edu.tpi.trabajos.dominio.UmbralClimatico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pronósticos y umbrales de prueba. */
final class Climas {

    private Climas() {
    }

    /** Umbrales de referencia del enunciado: viento 5-15, ráfaga 20, temp 30, humedad 50, 3 h, 7 días. */
    static UmbralClimatico umbral() {
        UmbralClimatico u = new UmbralClimatico();
        u.setVientoMinKmh(new BigDecimal("5"));
        u.setVientoMaxKmh(new BigDecimal("15"));
        u.setRafagaMaxKmh(new BigDecimal("20"));
        u.setTemperaturaMaxC(new BigDecimal("30"));
        u.setHumedadMinPct(new BigDecimal("50"));
        u.setHorasSinLluviaDespues(3);
        u.setHorizonteDias(7);
        return u;
    }

    static HoraClima apta(LocalDateTime h) {
        return new HoraClima(h, 10, 15, 0, 22, 65);
    }

    /** Pronóstico apto para todas las horas entre desde y hasta. */
    static Map<LocalDateTime, HoraClima> aptas(LocalDateTime desde, LocalDateTime hasta) {
        Map<LocalDateTime, HoraClima> m = new LinkedHashMap<>();
        for (LocalDateTime h = desde; h.isBefore(hasta); h = h.plusHours(1)) {
            m.put(h, apta(h));
        }
        return m;
    }
}
