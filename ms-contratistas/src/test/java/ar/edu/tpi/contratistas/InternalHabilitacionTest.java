package ar.edu.tpi.contratistas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static ar.edu.tpi.contratistas.Usuarios.C1;
import static ar.edu.tpi.contratistas.Usuarios.C2;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InternalHabilitacionTest {

    @Autowired
    MockMvc mvc;

    long maquina;
    long aplicador;

    @BeforeEach
    void setUp() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        maquina = Fixtures.maquina(mvc, C1);
        aplicador = Fixtures.aplicador(mvc, C1, "20111222");
    }

    ResultActions consultar(String fecha) throws Exception {
        // ms-trabajos propaga el token del usuario que opera (puede ser el productor al asignar)
        return mvc.perform(get("/internal/maquinas/" + maquina + "/habilitacion")
                .param("aplicadorId", String.valueOf(aplicador)).param("fecha", fecha).with(C2));
    }

    @Test
    void habilitadoConDocumentosValidadosYVigentes() throws Exception {
        Fixtures.aprobar(mvc, Fixtures.habilitacion(mvc, C1, "INSPECCION_MAQUINA", maquina, "2027-06-30"));
        Fixtures.aprobar(mvc, Fixtures.habilitacion(mvc, C1, "CARNET_APLICADOR", aplicador, "2027-06-30"));
        consultar("2026-12-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(true))
                .andExpect(jsonPath("$.contratistaSub").value("c1"))
                .andExpect(jsonPath("$.tipoMaquina").value("PULVERIZADORA"))
                .andExpect(jsonPath("$.rendimientoHaH").value(25))
                .andExpect(jsonPath("$.baseLat").value(-31.4));
    }

    @Test
    void documentosPendientesDevuelvenMotivo() throws Exception {
        Fixtures.habilitacion(mvc, C1, "INSPECCION_MAQUINA", maquina, "2027-06-30");
        consultar("2026-12-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.motivo", containsString("PENDIENTE")));
    }

    @Test
    void aplicadorDeOtraEmpresaNoHabilita() throws Exception {
        Fixtures.empresa(mvc, C2, "30798765432");
        aplicador = Fixtures.aplicador(mvc, C2, "20999888");
        consultar("2026-12-10").andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.motivo", containsString("no pertenece")));
    }

    @Test
    void maquinaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/internal/maquinas/999/habilitacion").param("aplicadorId", String.valueOf(aplicador))
                .param("fecha", "2026-12-10").with(C1)).andExpect(status().isNotFound());
    }

    @Test
    void fechaMalFormadaDevuelve400() throws Exception {
        consultar("10/12/2026").andExpect(status().isBadRequest());
    }
}
