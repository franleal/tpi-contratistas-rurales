package ar.edu.tpi.contratistas;

import org.junit.jupiter.api.BeforeEach;
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

/**
 * Tarjeta 03-14: el rol lo controla el Gateway, la propiedad la controla el servicio. Un
 * contratista no puede ver ni operar recursos de otro (403 con el formato común).
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PropiedadRecursosTest {

    @Autowired
    MockMvc mvc;

    long maquinaC1;
    long aplicadorC1;

    @BeforeEach
    void setUp() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        Fixtures.empresa(mvc, C2, "30798765432");
        maquinaC1 = Fixtures.maquina(mvc, C1);
        aplicadorC1 = Fixtures.aplicador(mvc, C1, "20111222");
        Fixtures.bloque(mvc, C1, maquinaC1, "2026-12-10T06:00:00", "2026-12-10T20:00:00");
    }

    @Test
    void noPuedeDeclararBloquesEnMaquinaAjena() throws Exception {
        mvc.perform(post("/api/agenda/bloques").with(C2).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"maquinaId":%d,"desde":"2026-12-11T06:00:00","hasta":"2026-12-11T20:00:00"}"""
                                .formatted(maquinaC1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"))
                .andExpect(jsonPath("$.path").value("/api/agenda/bloques"));
        mvc.perform(get("/api/agenda/bloques").param("maquinaId", String.valueOf(maquinaC1)).with(C1))
                .andExpect(jsonPath("$.bloques", hasSize(1)));
    }

    @Test
    void noPuedeConsultarLaAgendaDeMaquinaAjena() throws Exception {
        mvc.perform(get("/api/agenda/bloques").param("maquinaId", String.valueOf(maquinaC1)).with(C2))
                .andExpect(status().isForbidden());
    }

    @Test
    void noPuedeCargarHabilitacionesDeRecursosAjenos() throws Exception {
        for (String body : new String[]{
                """
                {"tipo":"INSPECCION_MAQUINA","titularId":%d,"numero":"X","vencimiento":"2027-01-01"}""".formatted(maquinaC1),
                """
                {"tipo":"CARNET_APLICADOR","titularId":%d,"numero":"X","vencimiento":"2027-01-01"}""".formatted(aplicadorC1)}) {
            mvc.perform(post("/api/habilitaciones").with(C2).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void losListadosNoMuestranRecursosAjenos() throws Exception {
        mvc.perform(get("/api/maquinas").with(C2)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/aplicadores").with(C2)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void elInternoDevuelveElDuenioParaQueTrabajosApliquePropiedad() throws Exception {
        // la regla "la máquina de la postulación es del contratista que se postula" la aplica
        // ms-trabajos comparando contratistaSub con el sub del token
        mvc.perform(get("/internal/maquinas/" + maquinaC1 + "/habilitacion")
                        .param("aplicadorId", String.valueOf(aplicadorC1)).param("fecha", "2026-12-10").with(C2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contratistaSub").value("c1"));
    }

    @Test
    void sinTokenDevuelve401ConFormatoComun() throws Exception {
        mvc.perform(get("/api/maquinas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
