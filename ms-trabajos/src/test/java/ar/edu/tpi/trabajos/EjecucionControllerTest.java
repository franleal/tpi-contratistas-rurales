package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.Zona;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;

import static ar.edu.tpi.trabajos.Usuarios.C1;
import static ar.edu.tpi.trabajos.Usuarios.C2;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F-16 / RN-15. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojFijo.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EjecucionControllerTest extends EjecucionBase {

    @BeforeEach
    void setUp() {
        orden = ordenAsignada("p1", "c1");
    }

    @Test
    void registraYLaOrdenPasaAEjecutada() throws Exception {
        registrar(C1, INICIO, INICIO.plusHours(4), "98.5")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orden.estado").value("EJECUTADA"))
                .andExpect(jsonPath("$.ejecucion.hectareas").value(98.5))
                .andExpect(jsonPath("$.asignacion.contratistaSub").value("c1"));
    }

    @Test
    void contratistaNoAsignadoDevuelve403() throws Exception {
        registrar(C2, INICIO, INICIO.plusHours(4), "50").andExpect(status().isForbidden());
    }

    @Test
    void finAnteriorAlInicioDevuelve422() throws Exception {
        registrar(C1, INICIO, INICIO.minusHours(1), "50").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void finEnElFuturoDevuelve422() throws Exception {
        registrar(C1, RelojFijo.AHORA, RelojFijo.AHORA.plusHours(2), "50").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("futuro")));
    }

    @Test
    void hectareasMayoresALaSuperficieDevuelve422() throws Exception {
        registrar(C1, INICIO, INICIO.plusHours(4), "100.01").andExpect(status().isUnprocessableEntity());
        registrar(C1, INICIO, INICIO.plusHours(4), "0").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void ordenNoAsignadaDevuelve409() throws Exception {
        orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 14), EstadoOrden.ABIERTA)).getId();
        registrar(C1, INICIO, INICIO.plusHours(4), "50").andExpect(status().isConflict());
    }

    @Test
    void segundoRegistroDevuelve409() throws Exception {
        registrar(C1, INICIO, INICIO.plusHours(4), "50").andExpect(status().isCreated());
        registrar(C1, INICIO, INICIO.plusHours(4), "50").andExpect(status().isConflict());
    }
}
