package ar.edu.tpi.campos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** Carga el data.sql real y verifica los lotes de demostración. */
@SpringBootTest(properties = "spring.sql.init.mode=always")
@AutoConfigureMockMvc
@DirtiesContext
class DatosDemoTest {

    @Autowired
    MockMvc mvc;

    @Test
    void losLotesDeDemostracionTienenSuZona() throws Exception {
        var token = jwt().jwt(j -> j.subject("x"));
        mvc.perform(get("/internal/lotes/1").with(token))
                .andExpect(jsonPath("$.productorSub").value("11111111-0000-0000-0000-000000000001"))
                .andExpect(jsonPath("$.zona").value("LIBRE"));
        mvc.perform(get("/internal/lotes/2").with(token)).andExpect(jsonPath("$.zona").value("AMORTIGUAMIENTO"));
        mvc.perform(get("/internal/lotes/3").with(token)).andExpect(jsonPath("$.admiteOrdenes").value(false));
    }
}
