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

    /**
     * Respuesta cruda de Open-Meteo desde la marca {@code desde}, una marca por elemento. Lluvia y
     * ráfaga van tal como las informa Open-Meteo: el valor de la marca H corresponde a [H - 1, H).
     * Si una serie es null se usan valores aptos (sin lluvia, ráfaga 15).
     */
    static String openMeteo(String desde, double[] lluvia, double[] rafaga) {
        int n = lluvia != null ? lluvia.length : rafaga.length;
        LocalDateTime t = LocalDateTime.parse(desde);
        StringBuilder time = new StringBuilder();
        StringBuilder pp = new StringBuilder();
        StringBuilder gu = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        StringBuilder hum = new StringBuilder();
        StringBuilder vel = new StringBuilder();
        for (int i = 0; i < n; i++) {
            String sep = i == 0 ? "" : ",";
            time.append(sep).append('"').append(t.plusHours(i)).append('"');
            pp.append(sep).append(lluvia != null ? lluvia[i] : 0);
            gu.append(sep).append(rafaga != null ? rafaga[i] : 15);
            temp.append(sep).append(22);
            hum.append(sep).append(65);
            vel.append(sep).append(10);
        }
        return ("{\"hourly\":{\"time\":[%s],\"temperature_2m\":[%s],\"relative_humidity_2m\":[%s],"
                + "\"precipitation\":[%s],\"wind_speed_10m\":[%s],\"wind_gusts_10m\":[%s]}}")
                .formatted(time, temp, hum, pp, vel, gu);
    }
}
