package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.dominio.EstadoMatricula;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.AgronomoRepository;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.RecetaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static ar.edu.tpi.trabajos.Usuarios.A1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tarjeta 04-09 (RN-18): la API responde 409 cuando la orden no está en el estado esperado. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TransicionesApiTest {

    static final LocalDate LIMITE = LocalDate.now().plusDays(10);

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @Autowired
    AgronomoRepository agronomos;
    @Autowired
    RecetaRepository recetas;

    @BeforeEach
    void setUp() {
        agronomos.save(Datos.agronomo("a1", EstadoMatricula.VALIDADA, LIMITE.plusYears(1)));
    }

    ResultActions firmar(long orden) throws Exception {
        return mvc.perform(post("/api/ordenes/" + orden + "/receta").with(A1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"producto\":\"P\",\"categoriaToxicologica\":\"IV\",\"dosis\":\"1 l/ha\"}"));
    }

    @Test
    void segundaRecetaSobreLaMismaOrdenDevuelve409() throws Exception {
        long id = ordenes.save(Datos.orden("p1", Zona.LIBRE, LIMITE, EstadoOrden.RECETA_PENDIENTE)).getId();
        firmar(id).andExpect(status().isCreated());
        firmar(id).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"))
                .andExpect(jsonPath("$.mensaje", containsString("ABIERTA")));
        assertThat(recetas.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrden.class, names = "RECETA_PENDIENTE", mode = EnumSource.Mode.EXCLUDE)
    void firmarEnCualquierOtroEstadoDevuelve409(EstadoOrden estado) throws Exception {
        long id = ordenes.save(Datos.orden("p1", Zona.LIBRE, LIMITE, estado)).getId();
        firmar(id).andExpect(status().isConflict()).andExpect(jsonPath("$.mensaje", containsString("RN-18")));
        assertThat(ordenes.findById(id).orElseThrow().getEstado()).isEqualTo(estado);
    }
}
