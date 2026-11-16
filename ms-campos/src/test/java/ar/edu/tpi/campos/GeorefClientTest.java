package ar.edu.tpi.campos;

import ar.edu.tpi.campos.cliente.GeorefClient;
import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

class GeorefClientTest {

    private MockRestServiceServer server;
    private GeorefClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://georef");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeorefClient(builder.build());
    }

    @Test
    void ubicaUnPuntoDeCordoba() {
        server.expect(requestTo(startsWith("http://georef/ubicacion")))
                .andExpect(queryParam("lat", "-31.65"))
                .andRespond(withSuccess("""
                        {"ubicacion":{"departamento":{"id":"14119","nombre":"Río Segundo"},
                         "provincia":{"id":"14","nombre":"Córdoba"}}}""", MediaType.APPLICATION_JSON));

        GeorefClient.Ubicacion u = client.ubicar(-31.65, -63.9).orElseThrow();

        assertThat(u.provincia()).isEqualTo("Córdoba");
        assertThat(u.departamentoId()).isEqualTo("14119");
    }

    @Test
    void puntoFueraDeArgentinaDevuelveVacio() {
        server.expect(requestTo(startsWith("http://georef/ubicacion")))
                .andRespond(withSuccess("""
                        {"ubicacion":{"departamento":{"id":null,"nombre":null},
                         "provincia":{"id":null,"nombre":null}}}""", MediaType.APPLICATION_JSON));

        assertThat(client.ubicar(-20, -40)).isEmpty();
    }

    @Test
    void errorDeGeorefEs503() {
        server.expect(requestTo(startsWith("http://georef/ubicacion"))).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.ubicar(-31, -64)).isInstanceOf(DependenciaNoDisponibleException.class);
    }
}
