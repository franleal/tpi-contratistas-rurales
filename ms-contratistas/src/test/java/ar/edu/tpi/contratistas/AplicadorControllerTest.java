package ar.edu.tpi.contratistas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static ar.edu.tpi.contratistas.Usuarios.C1;
import static ar.edu.tpi.contratistas.Usuarios.C2;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AplicadorControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void registraAplicador() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        mvc.perform(post("/api/aplicadores").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Juan Pérez","documento":"20111222"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Juan Pérez"));
    }

    @Test
    void documentoRepetidoEnLaMismaEmpresaDevuelve409() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        Fixtures.aplicador(mvc, C1, "20111222");
        mvc.perform(post("/api/aplicadores").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Otro","documento":"20111222"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void documentoInvalidoDevuelve400() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        mvc.perform(post("/api/aplicadores").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"X","documento":"20.111.222"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaSoloLosPropios() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        Fixtures.empresa(mvc, C2, "30798765432");
        Fixtures.aplicador(mvc, C1, "20111222");
        Fixtures.aplicador(mvc, C2, "20111222");
        mvc.perform(get("/api/aplicadores").with(C1)).andExpect(jsonPath("$", hasSize(1)));
    }
}
