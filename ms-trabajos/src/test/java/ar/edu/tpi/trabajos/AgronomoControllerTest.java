package ar.edu.tpi.trabajos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static ar.edu.tpi.trabajos.Usuarios.A1;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AgronomoControllerTest {

    @Autowired
    MockMvc mvc;

    static final String BODY = """
            {"matricula":"CIAC-1234","provincia":"Córdoba","vencimiento":"2027-12-31"}""";

    @Test
    void registraMatriculaPendienteYLaConsulta() throws Exception {
        mvc.perform(post("/api/agronomos").with(A1).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.nombre").value("Usuario a1"));
        mvc.perform(get("/api/agronomos/me").with(A1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matricula").value("CIAC-1234"));
    }

    @Test
    void segundoRegistroDevuelve409() throws Exception {
        mvc.perform(post("/api/agronomos").with(A1).contentType(MediaType.APPLICATION_JSON).content(BODY));
        mvc.perform(post("/api/agronomos").with(A1).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void sinMatriculaRegistradaDevuelve404() throws Exception {
        mvc.perform(get("/api/agronomos/me").with(A1)).andExpect(status().isNotFound());
    }

    @Test
    void datosIncompletosDevuelven400() throws Exception {
        mvc.perform(post("/api/agronomos").with(A1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"matricula\":\"X\"}")).andExpect(status().isBadRequest());
    }
}
