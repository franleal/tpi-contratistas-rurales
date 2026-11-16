package ar.edu.tpi.trabajos;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static ar.edu.tpi.trabajos.Usuarios.A1;
import static ar.edu.tpi.trabajos.Usuarios.ADMIN;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminMatriculaControllerTest {

    @Autowired
    MockMvc mvc;

    long id;

    @BeforeEach
    void setUp() throws Exception {
        String body = mvc.perform(post("/api/agronomos").with(A1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matricula":"CIAC-1","provincia":"Córdoba","vencimiento":"2027-12-31"}"""))
                .andReturn().getResponse().getContentAsString();
        id = ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void listaPendientesYAprueba() throws Exception {
        mvc.perform(get("/api/admin/matriculas").param("estado", "PENDIENTE").with(ADMIN))
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(post("/api/admin/matriculas/" + id + "/aprobar").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDADA"));
        mvc.perform(get("/api/agronomos/me").with(A1)).andExpect(jsonPath("$.estado").value("VALIDADA"));
    }

    @Test
    void rechazaConMotivoYNoSePuedeVolverAResolver() throws Exception {
        mvc.perform(post("/api/admin/matriculas/" + id + "/rechazar").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Matrícula de otra provincia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"));
        mvc.perform(post("/api/admin/matriculas/" + id + "/aprobar").with(ADMIN)).andExpect(status().isConflict());
    }

    @Test
    void inexistenteDevuelve404() throws Exception {
        mvc.perform(post("/api/admin/matriculas/999/aprobar").with(ADMIN)).andExpect(status().isNotFound());
    }
}
