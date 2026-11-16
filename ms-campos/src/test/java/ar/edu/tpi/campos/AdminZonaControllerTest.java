package ar.edu.tpi.campos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Carga el data.sql real para verificar que su sintaxis es válida. */
@SpringBootTest(properties = "spring.sql.init.mode=always")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminZonaControllerTest {

    private static final RequestPostProcessor ADMIN = jwt().jwt(j -> j.subject("admin1"));

    @Autowired
    private MockMvc mvc;

    @Test
    void listaLosParametrosPrecargados() throws Exception {
        mvc.perform(get("/api/admin/zonas").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[?(@.provincia == 'Córdoba')].distanciaExclusionM", hasItem(1500)))
                .andExpect(jsonPath("$[0].categoriasPermitidas", hasSize(2)));
    }

    @Test
    void actualizaLosParametrosDeUnaProvincia() throws Exception {
        mvc.perform(put("/api/admin/zonas/Córdoba").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"distanciaExclusionM":800,"distanciaAmortiguamientoM":2500,"categoriasPermitidas":["iv","III"]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanciaExclusionM").value(800))
                .andExpect(jsonPath("$.categoriasPermitidas[0]").value("III"))
                .andExpect(jsonPath("$.categoriasPermitidas[1]").value("IV"));
    }

    @Test
    void exclusionMayorQueAmortiguamientoDevuelve400() throws Exception {
        mvc.perform(put("/api/admin/zonas/Córdoba").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"distanciaExclusionM":3000,"distanciaAmortiguamientoM":1000,"categoriasPermitidas":["IV"]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }

    @Test
    void categoriaInexistenteDevuelve400() throws Exception {
        mvc.perform(put("/api/admin/zonas/Córdoba").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"distanciaExclusionM":500,"distanciaAmortiguamientoM":1000,"categoriasPermitidas":["V"]}"""))
                .andExpect(status().isBadRequest());
    }
}
