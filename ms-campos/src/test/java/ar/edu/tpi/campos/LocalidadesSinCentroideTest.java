package ar.edu.tpi.campos;

import ar.edu.tpi.campos.dominio.ParametroZona;
import ar.edu.tpi.campos.repositorio.LoteRepository;
import ar.edu.tpi.campos.repositorio.ParametroZonaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RN-05 / RN-08: con el GeorefClient real, si Georef lista localidades sin coordenadas la
 * respuesta es incompleta: 503 y el lote no se da de alta (antes se filtraban y podía quedar LIBRE).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(LocalidadesSinCentroideTest.GeorefSimulado.class)
@DirtiesContext
class LocalidadesSinCentroideTest {

    static final RequestPostProcessor P1 = jwt().jwt(j -> j.subject("p1").claim("preferred_username", "p1"));

    @TestConfiguration
    static class GeorefSimulado {
        final RestClient.Builder builder = RestClient.builder().baseUrl("http://georef");
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        @Bean
        MockRestServiceServer georefServer() {
            return server;
        }

        @Bean
        @Primary
        @Qualifier("georefRestClient")
        RestClient georefRestClientSimulado() {
            return builder.build();
        }
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    LoteRepository lotes;
    @Autowired
    ParametroZonaRepository parametros;
    @Autowired
    MockRestServiceServer server;

    @Test
    void localidadesSinCoordenadasDevuelve503SinGuardarElLote() throws Exception {
        parametros.save(new ParametroZona("Córdoba", 1000, 3000, List.of("III", "IV")));
        String body = mvc.perform(post("/api/establecimientos").with(P1).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Campo","provincia":"Córdoba"}""")).andReturn().getResponse().getContentAsString();
        long est = ((Number) JsonPath.read(body, "$.id")).longValue();

        server.expect(requestTo(startsWith("http://georef/ubicacion"))).andRespond(withSuccess("""
                {"ubicacion":{"departamento":{"id":"14119","nombre":"Río Segundo"},
                 "provincia":{"id":"14","nombre":"Córdoba"}}}""", MediaType.APPLICATION_JSON));
        server.expect(requestTo(startsWith("http://georef/localidades"))).andRespond(withSuccess("""
                {"localidades":[{"id":"1","nombre":"Pilar","centroide":null},
                                {"id":"2","nombre":"Rio Segundo","centroide":{"lat":null,"lon":null}}]}""",
                MediaType.APPLICATION_JSON));

        mvc.perform(post("/api/lotes").with(P1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"establecimientoId":%d,"nombre":"L","lat":-31.6,"lon":-63.9,"superficieHa":50,"cultivo":"Maíz"}"""
                                .formatted(est)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("DEPENDENCIA_NO_DISPONIBLE"));

        server.verify();
        assertThat(lotes.count()).isZero();
    }
}
