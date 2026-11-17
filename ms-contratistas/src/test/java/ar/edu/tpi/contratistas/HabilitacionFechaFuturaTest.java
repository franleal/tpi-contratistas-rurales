package ar.edu.tpi.contratistas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static ar.edu.tpi.contratistas.Usuarios.C1;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Tarjeta 03-13 (RN-04): la vigencia se calcula contra la fecha del trabajo, no contra hoy.
 * Fechas relativas a hoy para que la prueba no envejezca.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HabilitacionFechaFuturaTest {

    static final LocalDate HOY = LocalDate.now();

    @Autowired
    MockMvc mvc;

    long maquina;
    long aplicador;

    @BeforeEach
    void setUp() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        maquina = Fixtures.maquina(mvc, C1);
        aplicador = Fixtures.aplicador(mvc, C1, "20111222");
        Fixtures.aprobar(mvc, Fixtures.habilitacion(mvc, C1, "INSPECCION_MAQUINA", maquina,
                HOY.plusYears(1).toString()));
    }

    void carnetValidadoQueVence(LocalDate vencimiento) throws Exception {
        Fixtures.aprobar(mvc, Fixtures.habilitacion(mvc, C1, "CARNET_APLICADOR", aplicador, vencimiento.toString()));
    }

    ResultActions evaluarPara(LocalDate fechaTrabajo) throws Exception {
        return mvc.perform(get("/internal/maquinas/" + maquina + "/habilitacion")
                .param("aplicadorId", String.valueOf(aplicador)).param("fecha", fechaTrabajo.toString()).with(C1));
    }

    @Test
    void carnetVigenteHoyPeroVencidoParaElTrabajoNoHabilita() throws Exception {
        carnetValidadoQueVence(HOY.plusDays(2));
        evaluarPara(HOY).andExpect(jsonPath("$.habilitado").value(true));
        evaluarPara(HOY.plusDays(5))
                .andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.motivo", containsString(HOY.plusDays(2).toString())));
    }

    @Test
    void carnetQueVenceElMismoDiaDelTrabajoHabilita() throws Exception {
        carnetValidadoQueVence(HOY.plusDays(5));
        evaluarPara(HOY.plusDays(5)).andExpect(jsonPath("$.habilitado").value(true));
        evaluarPara(HOY.plusDays(6)).andExpect(jsonPath("$.habilitado").value(false));
    }

    @Test
    void renovarElCarnetVuelveAHabilitarLaFechaFutura() throws Exception {
        carnetValidadoQueVence(HOY.plusDays(2));
        evaluarPara(HOY.plusDays(10)).andExpect(jsonPath("$.habilitado").value(false));
        carnetValidadoQueVence(HOY.plusYears(1));
        evaluarPara(HOY.plusDays(10)).andExpect(jsonPath("$.habilitado").value(true));
    }

    @Test
    void renovacionPendienteNoAlcanzaParaLaFechaFutura() throws Exception {
        carnetValidadoQueVence(HOY.plusDays(2));
        Fixtures.habilitacion(mvc, C1, "CARNET_APLICADOR", aplicador, HOY.plusYears(1).toString());
        evaluarPara(HOY.plusDays(10))
                .andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.motivo", containsString("vence")));
    }

    @Test
    void inspeccionVencidaParaElTrabajoNoHabilitaAunqueElCarnetSi() throws Exception {
        carnetValidadoQueVence(HOY.plusYears(3));
        evaluarPara(HOY.plusYears(2))
                .andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.motivo", containsString("RN-03")));
    }
}
