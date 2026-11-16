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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MaquinaControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void registraPulverizadoraPorDefecto() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        mvc.perform(post("/api/maquinas").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descripcion":"Pla MAP 3","anchoM":28,"rendimientoHaH":25.5,"baseLat":-31.4,"baseLon":-64.18}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PULVERIZADORA"))
                .andExpect(jsonPath("$.rendimientoHaH").value(25.5))
                .andExpect(jsonPath("$.inspeccion").value(nullValue()));
    }

    @Test
    void sinEmpresaRegistradaDevuelve422() throws Exception {
        mvc.perform(post("/api/maquinas").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descripcion":"X","anchoM":28,"rendimientoHaH":25,"baseLat":-31.4,"baseLon":-64.18}"""))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rendimientoCeroDevuelve400() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        mvc.perform(post("/api/maquinas").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descripcion":"X","anchoM":28,"rendimientoHaH":0,"baseLat":-31.4,"baseLon":-64.18}"""))
                .andExpect(status().isBadRequest());
    }

    /** RN-11: 0.001 es positivo pero se guardaría como 0.00; se rechaza antes de guardar. */
    @Test
    void rendimientoConMasDeDosDecimalesDevuelve400() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        mvc.perform(post("/api/maquinas").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descripcion":"X","anchoM":28,"rendimientoHaH":0.001,"baseLat":-31.4,"baseLon":-64.18}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rendimientoDeUnCentesimoSeAcepta() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        mvc.perform(post("/api/maquinas").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"descripcion":"X","anchoM":28,"rendimientoHaH":0.01,"baseLat":-31.4,"baseLon":-64.18}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rendimientoHaH").value(0.01));
    }

    @Test
    void listaSoloLasPropias() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        Fixtures.empresa(mvc, C2, "30798765432");
        Fixtures.maquina(mvc, C1);
        Fixtures.maquina(mvc, C2);
        Fixtures.maquina(mvc, C2);

        mvc.perform(get("/api/maquinas").with(C1)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/maquinas").with(C2)).andExpect(jsonPath("$", hasSize(2)));
    }
}
