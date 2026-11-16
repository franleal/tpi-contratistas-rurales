package ar.edu.tpi.trabajos.cliente;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pronóstico y clima observado por hora (Open-Meteo /v1/forecast, hora local de Argentina).
 * Falla segura: cualquier error, timeout o respuesta incompleta es 503 DEPENDENCIA_NO_DISPONIBLE.
 *
 * <p>Cada {@link HoraClima} describe el intervalo [hora, hora + 1 h). Open-Meteo informa
 * temperatura, humedad y viento como valores instantáneos en la marca horaria, pero lluvia y
 * ráfagas como suma/máximo de la <em>hora anterior</em> a la marca. Por eso la lluvia y la ráfaga
 * del intervalo [H, H + 1) se toman de la marca H + 1 (RN-09, RN-16), y se pide un día más para
 * cubrir la última hora del rango.</p>
 */
@Component
public class OpenMeteoClient {

    public static final String ZONA_HORARIA = "America/Argentina/Buenos_Aires";
    static final String VARIABLES =
            "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,wind_gusts_10m";

    private final RestClient http;

    public OpenMeteoClient(@Qualifier("openMeteoRestClient") RestClient http) {
        this.http = http;
    }

    /** Valores de una hora. Velocidades en km/h, precipitación en mm, temperatura en °C, humedad en %. */
    public record HoraClima(LocalDateTime hora, double vientoKmh, double rafagaKmh, double precipitacionMm,
                            double temperaturaC, double humedadPct) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Respuesta(Horario hourly) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Horario(List<String> time,
                   @JsonProperty("temperature_2m") List<Double> temperatura,
                   @JsonProperty("relative_humidity_2m") List<Double> humedad,
                   @JsonProperty("precipitation") List<Double> precipitacion,
                   @JsonProperty("wind_speed_10m") List<Double> viento,
                   @JsonProperty("wind_gusts_10m") List<Double> rafaga) {
    }

    /** Horas entre las fechas indicadas (inclusive), ordenadas e indexadas por el inicio del intervalo. */
    public Map<LocalDateTime, HoraClima> horas(double lat, double lon, LocalDate desde, LocalDate hasta) {
        Respuesta r;
        try {
            r = http.get().uri(u -> u.path("/v1/forecast")
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lon)
                            .queryParam("hourly", VARIABLES)
                            .queryParam("start_date", desde)
                            .queryParam("end_date", hasta.plusDays(1)) // lluvia/ráfaga de la última hora
                            .queryParam("timezone", ZONA_HORARIA)
                            .build())
                    .retrieve().body(Respuesta.class);
        } catch (RestClientException e) {
            throw new DependenciaNoDisponibleException("Open-Meteo no está disponible", e);
        }
        return convertir(r);
    }

    static Map<LocalDateTime, HoraClima> convertir(Respuesta r) {
        if (r == null || r.hourly() == null || r.hourly().time() == null) {
            throw new DependenciaNoDisponibleException("Open-Meteo devolvió una respuesta sin datos horarios");
        }
        Horario h = r.hourly();
        int n = h.time().size();
        for (List<Double> serie : List.of(lista(h.temperatura()), lista(h.humedad()), lista(h.precipitacion()),
                lista(h.viento()), lista(h.rafaga()))) {
            if (serie.size() != n) {
                throw new DependenciaNoDisponibleException("Open-Meteo devolvió series horarias incompletas");
            }
        }
        // índice de cada marca horaria, para leer lluvia y ráfaga en la marca siguiente
        List<LocalDateTime> marcas = new ArrayList<>(n);
        Map<LocalDateTime, Integer> indice = new HashMap<>();
        try {
            for (int i = 0; i < n; i++) {
                LocalDateTime hora = LocalDateTime.parse(h.time().get(i));
                marcas.add(hora);
                indice.put(hora, i);
            }
        } catch (DateTimeParseException | NullPointerException e) {
            throw new DependenciaNoDisponibleException("Open-Meteo devolvió una hora inválida", e);
        }
        Map<LocalDateTime, HoraClima> horas = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            LocalDateTime hora = marcas.get(i);
            Integer j = indice.get(hora.plusHours(1)); // lluvia y ráfaga de [hora, hora + 1)
            if (j == null || h.temperatura().get(i) == null || h.humedad().get(i) == null
                    || h.viento().get(i) == null || h.precipitacion().get(j) == null || h.rafaga().get(j) == null) {
                continue; // hora sin dato: quien la necesite la verá como faltante
            }
            horas.put(hora, new HoraClima(hora, h.viento().get(i), h.rafaga().get(j), h.precipitacion().get(j),
                    h.temperatura().get(i), h.humedad().get(i)));
        }
        return horas;
    }

    private static List<Double> lista(List<Double> l) {
        return l == null ? new ArrayList<>() : l;
    }
}
