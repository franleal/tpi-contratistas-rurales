package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.servicio.EvaluadorClima;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenMeteoClientTest {

    MockRestServiceServer server;
    OpenMeteoClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder b = RestClient.builder().baseUrl("http://meteo");
        server = MockRestServiceServer.bindTo(b).build();
        cliente = new OpenMeteoClient(b.build());
    }

    @Test
    void convierteLasSeriesHorarias() {
        server.expect(requestTo(allOf(containsString("/v1/forecast"), containsString("start_date=2026-12-10"),
                        containsString("end_date=2026-12-11"), containsString("wind_gusts_10m"),
                        containsString("timezone=America"))))
                .andRespond(withSuccess("""
                        {"hourly":{"time":["2026-12-10T08:00","2026-12-10T09:00","2026-12-10T10:00"],
                         "temperature_2m":[22.1,24.0,25],"relative_humidity_2m":[70,65,60],"precipitation":[0,0.2,0],
                         "wind_speed_10m":[8.5,10.1,11],"wind_gusts_10m":[14,18.3,16]}}""", MediaType.APPLICATION_JSON));
        var horas = cliente.horas(-31.4, -64.2, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 10));
        // la última marca sólo aporta lluvia/ráfaga de la hora previa: no hay intervalo [10, 11) completo
        assertThat(horas).hasSize(2);
        var h9 = horas.get(LocalDateTime.of(2026, 12, 10, 9, 0));
        assertThat(h9.temperaturaC()).isEqualTo(24.0);
        assertThat(h9.vientoKmh()).isEqualTo(10.1);
        assertThat(h9.precipitacionMm()).isEqualTo(0.0);
        assertThat(h9.rafagaKmh()).isEqualTo(16.0);
    }

    /** Open-Meteo informa lluvia y ráfaga de la hora anterior a la marca: se corren al intervalo [H, H + 1). */
    @Test
    void lluviaYRafagaSeAsignanAlIntervaloQueLasPrecede() {
        server.expect(requestTo(containsString("/v1/forecast"))).andRespond(withSuccess(Climas.openMeteo(
                "2026-12-10T07:00", new double[]{0, 0, 0, 1.4, 0}, new double[]{14, 30, 15, 16, 15}),
                MediaType.APPLICATION_JSON));
        var horas = cliente.horas(-31.4, -64.2, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 10));
        // marca 08:00 = ráfaga de 07-08; marca 10:00 = lluvia de 09-10
        assertThat(horas.get(LocalDateTime.of(2026, 12, 10, 7, 0)).rafagaKmh()).isEqualTo(30.0);
        assertThat(horas.get(LocalDateTime.of(2026, 12, 10, 8, 0)).rafagaKmh()).isEqualTo(15.0);
        assertThat(horas.get(LocalDateTime.of(2026, 12, 10, 8, 0)).precipitacionMm()).isZero();
        assertThat(horas.get(LocalDateTime.of(2026, 12, 10, 9, 0)).precipitacionMm()).isEqualTo(1.4);
        assertThat(horas.get(LocalDateTime.of(2026, 12, 10, 11, 0))).isNull(); // última marca: sin [11, 12)
    }

    /** Aplicación de 08:00 a 10:00 con lluvia sólo de 09:00 a 10:00: la postulación se rechaza (RN-09). */
    @Test
    void lluviaDeLaUltimaHoraRechazaLaAplicacion() {
        server.expect(requestTo(containsString("/v1/forecast"))).andRespond(withSuccess(Climas.openMeteo(
                "2026-12-10T08:00", new double[]{0, 0, 1.4, 0, 0, 0, 0}, null), MediaType.APPLICATION_JSON));
        var horas = cliente.horas(-31.4, -64.2, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 10));
        var u = Climas.umbral();
        u.setHorasSinLluviaDespues(0); // aislar la lluvia dentro de la aplicación
        var r = new EvaluadorClima().evaluar(horas, LocalDateTime.of(2026, 12, 10, 8, 0),
                LocalDateTime.of(2026, 12, 10, 10, 0), u);
        assertThat(r.apta()).isFalse();
        assertThat(r.primeraHoraQueFalla()).isEqualTo(LocalDateTime.of(2026, 12, 10, 9, 0));
        assertThat(r.motivo()).contains("lluvia");
    }

    /** Una ráfaga fuerte de 07:00 a 08:00 (marca 08:00) no afecta una aplicación que empieza a las 08:00. */
    @Test
    void rafagaDeLaHoraPreviaNoAfectaLaAplicacion() {
        server.expect(requestTo(containsString("/v1/forecast"))).andRespond(withSuccess(Climas.openMeteo(
                "2026-12-10T07:00", null, new double[]{15, 35, 15, 15, 15, 15, 15, 15}), MediaType.APPLICATION_JSON));
        var horas = cliente.horas(-31.4, -64.2, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 10));
        var u = Climas.umbral();
        u.setHorasSinLluviaDespues(1);
        var r = new EvaluadorClima().evaluar(horas, LocalDateTime.of(2026, 12, 10, 8, 0),
                LocalDateTime.of(2026, 12, 10, 10, 0), u);
        assertThat(r.apta()).isTrue();
    }

    @Test
    void errorDelServidorEs503() {
        server.expect(requestTo(containsString("/v1/forecast"))).andRespond(withServerError());
        assertThatThrownBy(() -> cliente.horas(-31, -64, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @Test
    void respuestaSinHorarioEs503() {
        server.expect(requestTo(containsString("/v1/forecast")))
                .andRespond(withSuccess("{\"error\":false}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> cliente.horas(-31, -64, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @Test
    void seriesDeDistintoLargoEs503() {
        server.expect(requestTo(containsString("/v1/forecast"))).andRespond(withSuccess("""
                {"hourly":{"time":["2026-12-10T08:00","2026-12-10T09:00"],"temperature_2m":[22],
                 "relative_humidity_2m":[70,65],"precipitation":[0,0],"wind_speed_10m":[8,9],"wind_gusts_10m":[14,15]}}""",
                MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> cliente.horas(-31, -64, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @Test
    void respuestaQueNoEsJsonEs503() {
        server.expect(requestTo(containsString("/v1/forecast")))
                .andRespond(withSuccess("<html>mantenimiento</html>", MediaType.TEXT_HTML));
        assertThatThrownBy(() -> cliente.horas(-31, -64, LocalDate.now(), LocalDate.now()))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }
}
