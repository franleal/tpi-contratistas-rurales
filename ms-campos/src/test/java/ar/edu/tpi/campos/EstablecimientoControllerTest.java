package ar.edu.tpi.campos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EstablecimientoControllerTest {

    @Autowired
    private MockMvc mvc;

    static RequestPostProcessor productor(String sub) {
        return jwt().jwt(j -> j.subject(sub).claim("preferred_username", sub));
    }

    @Test
    void creaEstablecimientoYDaDeAltaAlProductor() throws Exception {
        mvc.perform(post("/api/establecimientos").with(productor("p1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"La Esperanza","provincia":"Córdoba"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("La Esperanza"))
                .andExpect(jsonPath("$.lotes", hasSize(0)));
    }

    @Test
    void cadaProductorVeSoloLosPropios() throws Exception {
        mvc.perform(post("/api/establecimientos").with(productor("p1")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Del p1","provincia":"Córdoba"}""")).andExpect(status().isCreated());
        mvc.perform(post("/api/establecimientos").with(productor("p2")).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Del p2","provincia":"Santa Fe"}""")).andExpect(status().isCreated());

        mvc.perform(get("/api/establecimientos").with(productor("p1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Del p1"));
    }

    @Test
    void datosInvalidosDevuelven400() throws Exception {
        mvc.perform(post("/api/establecimientos").with(productor("p1")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.detalles", hasSize(2)));
    }
}
