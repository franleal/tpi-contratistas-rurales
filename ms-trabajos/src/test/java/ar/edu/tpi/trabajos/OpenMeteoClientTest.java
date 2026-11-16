package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
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
                        containsString("wind_gusts_10m"), containsString("timezone=America"))))
                .andRespond(withSuccess("""
                        {"hourly":{"time":["2026-12-10T08:00","2026-12-10T09:00"],
                         "temperature_2m":[22.1,24.0],"relative_humidity_2m":[70,65],"precipitation":[0,0.2],
                         "wind_speed_10m":[8.5,10.1],"wind_gusts_10m":[14,18.3]}}""", MediaType.APPLICATION_JSON));
        var horas = cliente.horas(-31.4, -64.2, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 10));
        assertThat(horas).hasSize(2);
        var h9 = horas.get(LocalDateTime.of(2026, 12, 10, 9, 0));
        assertThat(h9.precipitacionMm()).isEqualTo(0.2);
        assertThat(h9.rafagaKmh()).isEqualTo(18.3);
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
