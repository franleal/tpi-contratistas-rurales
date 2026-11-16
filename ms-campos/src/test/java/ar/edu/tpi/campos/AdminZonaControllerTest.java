package ar.edu.tpi.campos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Carga el data.sql real para verificar que su sintaxis es válida. */
@SpringBootTest(properties = "spring.sql.init.mode=always")
@AutoConfigureMockMvc
@DirtiesContext
class AdminZonaControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listaLosParametrosPrecargados() throws Exception {
        mvc.perform(get("/api/admin/zonas").with(jwt().jwt(j -> j.subject("admin1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[?(@.provincia == 'Córdoba')].distanciaExclusionM", hasItem(1500)))
                .andExpect(jsonPath("$[0].categoriasPermitidas", hasSize(2)));
    }
}
