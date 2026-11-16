package ar.edu.tpi.trabajos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static ar.edu.tpi.trabajos.Usuarios.ADMIN;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** Carga el data.sql real y verifica umbrales y matrículas de demostración. */
@SpringBootTest(properties = "spring.sql.init.mode=always")
@AutoConfigureMockMvc
@DirtiesContext
class DatosDemoTest {

    @Autowired
    MockMvc mvc;

    @Test
    void hayUnaMatriculaValidadaYUnaPendiente() throws Exception {
        mvc.perform(get("/api/admin/matriculas").with(ADMIN)).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/admin/matriculas").param("estado", "PENDIENTE").with(ADMIN))
                .andExpect(jsonPath("$[0].matricula").value("CIAC-7788"));
        mvc.perform(get("/api/admin/umbrales").with(ADMIN)).andExpect(jsonPath("$.horizonteDias").value(7));
    }
}
