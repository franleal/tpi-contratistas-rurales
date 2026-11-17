package ar.edu.tpi.trabajos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static ar.edu.tpi.trabajos.Usuarios.ADMIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Carga el data.sql real para verificar los umbrales precargados. */
@SpringBootTest(properties = "spring.sql.init.mode=always")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminUmbralControllerTest {

    @Autowired
    MockMvc mvc;

    ResultActions configurar(String json) throws Exception {
        return mvc.perform(put("/api/admin/umbrales").with(ADMIN).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void consultaLosUmbralesPrecargados() throws Exception {
        mvc.perform(get("/api/admin/umbrales").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vientoMaxKmh").value(15.0))
                .andExpect(jsonPath("$.horasSinLluviaDespues").value(3))
                .andExpect(jsonPath("$.horizonteDias").value(7));
    }

    @Test
    void actualizaLosUmbrales() throws Exception {
        configurar("""
                {"vientoMinKmh":3,"vientoMaxKmh":12,"rafagaMaxKmh":18,"temperaturaMaxC":28,"humedadMinPct":55,
                 "horasSinLluviaDespues":4,"horizonteDias":10}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horizonteDias").value(10));
        mvc.perform(get("/api/admin/umbrales").with(ADMIN)).andExpect(jsonPath("$.vientoMaxKmh").value(12.0));
    }

    @Test
    void horizonteMayorA16DiasDevuelve400() throws Exception {
        configurar("""
                {"vientoMinKmh":3,"vientoMaxKmh":12,"rafagaMaxKmh":18,"temperaturaMaxC":28,"humedadMinPct":55,
                 "horasSinLluviaDespues":4,"horizonteDias":17}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void vientoMinimoMayorQueMaximoDevuelve400() throws Exception {
        configurar("""
                {"vientoMinKmh":20,"vientoMaxKmh":12,"rafagaMaxKmh":25,"temperaturaMaxC":28,"humedadMinPct":55,
                 "horasSinLluviaDespues":4,"horizonteDias":7}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
    }
}
