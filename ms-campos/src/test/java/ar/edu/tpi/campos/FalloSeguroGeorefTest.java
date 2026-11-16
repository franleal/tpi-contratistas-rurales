package ar.edu.tpi.campos;

import ar.edu.tpi.campos.dominio.ParametroZona;
import ar.edu.tpi.campos.repositorio.LoteRepository;
import ar.edu.tpi.campos.repositorio.ParametroZonaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tarjeta 02-14 (RN-05, falla segura): con Georef inaccesible el registro de un lote responde
 * 503 con el formato común y no guarda nada. Usa el cliente HTTP real apuntando a un puerto
 * cerrado, igual que el override docker-compose.falla-georef.yml.
 */
@SpringBootTest(properties = {
        "georef.url=http://127.0.0.1:9",
        "http.connect-timeout-ms=500",
        "http.read-timeout-ms=500"})
@AutoConfigureMockMvc
@DirtiesContext
class FalloSeguroGeorefTest {

    static final RequestPostProcessor P1 = jwt().jwt(j -> j.subject("p1").claim("preferred_username", "p1"));

    @Autowired
    MockMvc mvc;
    @Autowired
    LoteRepository lotes;
    @Autowired
    ParametroZonaRepository parametros;

    @BeforeEach
    void setUp() {
        parametros.save(new ParametroZona("Córdoba", 1000, 3000, List.of("III", "IV")));
    }

    @Test
    void georefCaidoDevuelve503YNoGuardaElLote() throws Exception {
        String body = mvc.perform(post("/api/establecimientos").with(P1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Campo","provincia":"Córdoba"}"""))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long est = ((Number) JsonPath.read(body, "$.id")).longValue();
        long antes = lotes.count();

        mvc.perform(post("/api/lotes").with(P1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"establecimientoId":%d,"nombre":"L","lat":-31.6,"lon":-63.9,"superficieHa":50,"cultivo":"Maíz"}"""
                                .formatted(est)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("DEPENDENCIA_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.path").value("/api/lotes"))
                .andExpect(jsonPath("$.correlationId").exists());

        assertThat(lotes.count()).isEqualTo(antes);
    }
}
