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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ContratistaControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void registraLaEmpresaYLaConsulta() throws Exception {
        mvc.perform(post("/api/contratistas").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razonSocial":"Aplicaciones del Sur SRL","cuit":"30712345679"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cuit").value("30-71234567-9"));
        mvc.perform(get("/api/contratistas/me").with(C1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("Aplicaciones del Sur SRL"));
    }

    @Test
    void segundoRegistroDelMismoUsuarioDevuelve409() throws Exception {
        String body = """
                {"razonSocial":"A","cuit":"30-71234567-9"}""";
        mvc.perform(post("/api/contratistas").with(C1).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/contratistas").with(C1).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void cuitRepetidoDevuelve409() throws Exception {
        mvc.perform(post("/api/contratistas").with(C1).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"razonSocial":"A","cuit":"30-71234567-9"}""")).andExpect(status().isCreated());
        mvc.perform(post("/api/contratistas").with(C2).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razonSocial":"B","cuit":"30712345679"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void cuitMalFormadoDevuelve400() throws Exception {
        mvc.perform(post("/api/contratistas").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razonSocial":"A","cuit":"123"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinEmpresaRegistradaDevuelve422() throws Exception {
        mvc.perform(get("/api/contratistas/me").with(C1)).andExpect(status().isUnprocessableEntity());
    }
}
