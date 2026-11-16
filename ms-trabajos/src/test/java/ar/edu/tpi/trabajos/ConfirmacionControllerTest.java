package ar.edu.tpi.trabajos;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;

import static ar.edu.tpi.trabajos.Usuarios.C1;
import static ar.edu.tpi.trabajos.Usuarios.P1;
import static ar.edu.tpi.trabajos.Usuarios.P2;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F-17 / RN-17. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojFijo.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConfirmacionControllerTest extends EjecucionBase {

    @BeforeEach
    void setUp() {
        orden = ordenAsignada("p1", "c1");
        LocalDateTime d = INICIO.toLocalDate().atStartOfDay();
        when(meteo.horas(anyDouble(), anyDouble(), any(), any())).thenReturn(Climas.aptas(d, d.plusDays(1)));
    }

    @Test
    void confirmaLaEjecucionYCompletaLaOrden() throws Exception {
        registrar(C1, INICIO, INICIO.plusHours(4), "100").andExpect(status().isCreated());
        mvc.perform(post("/api/ordenes/" + orden + "/confirmacion").with(P1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden.estado").value("COMPLETADA"))
                .andExpect(jsonPath("$.ejecucion.confirmadaEn", notNullValue()));
    }

    @Test
    void sinEjecucionDevuelve409() throws Exception {
        mvc.perform(post("/api/ordenes/" + orden + "/confirmacion").with(P1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("RN-17")));
    }

    @Test
    void ordenAjenaDevuelve403() throws Exception {
        registrar(C1, INICIO, INICIO.plusHours(4), "100");
        mvc.perform(post("/api/ordenes/" + orden + "/confirmacion").with(P2)).andExpect(status().isForbidden());
    }

    @Test
    void confirmarDosVecesDevuelve409() throws Exception {
        registrar(C1, INICIO, INICIO.plusHours(4), "100");
        mvc.perform(post("/api/ordenes/" + orden + "/confirmacion").with(P1)).andExpect(status().isOk());
        mvc.perform(post("/api/ordenes/" + orden + "/confirmacion").with(P1)).andExpect(status().isConflict());
    }
}
