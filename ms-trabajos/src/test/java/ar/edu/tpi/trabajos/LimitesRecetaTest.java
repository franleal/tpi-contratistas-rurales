package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.dominio.EstadoMatricula;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.AgronomoRepository;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;

import static ar.edu.tpi.trabajos.Usuarios.A1;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tarjeta 04-10: casos límite de RN-01 (vencimiento de matrícula) y RN-07 (copia de categorías). */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LimitesRecetaTest {

    static final LocalDate LIMITE = LocalDate.now().plusDays(10);

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @Autowired
    AgronomoRepository agronomos;

    ResultActions firmar(long orden, String categoria) throws Exception {
        return mvc.perform(post("/api/ordenes/" + orden + "/receta").with(A1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"producto\":\"P\",\"categoriaToxicologica\":\"%s\",\"dosis\":\"1 l/ha\"}"
                        .formatted(categoria)));
    }

    @ParameterizedTest(name = "matrícula que vence {0} días respecto de la fecha límite -> {1}")
    @CsvSource({"-1, 422", "0, 201", "1, 201"})
    void vencimientoDeLaMatriculaContraLaFechaLimite(int dias, int esperado) throws Exception {
        agronomos.save(Datos.agronomo("a1", EstadoMatricula.VALIDADA, LIMITE.plusDays(dias)));
        long id = ordenes.save(Datos.orden("p1", Zona.LIBRE, LIMITE, EstadoOrden.RECETA_PENDIENTE)).getId();
        firmar(id, "IV").andExpect(status().is(esperado));
    }

    @Test
    void matriculaRechazadaDevuelve403() throws Exception {
        agronomos.save(Datos.agronomo("a1", EstadoMatricula.RECHAZADA, LIMITE.plusYears(1)));
        long id = ordenes.save(Datos.orden("p1", Zona.LIBRE, LIMITE, EstadoOrden.RECETA_PENDIENTE)).getId();
        firmar(id, "IV").andExpect(status().isForbidden());
    }

    @ParameterizedTest(name = "zona {0}, categoría {1} -> {2}")
    @CsvSource({
            "LIBRE, Ia, 201",
            "AMORTIGUAMIENTO, Ia, 422",
            "AMORTIGUAMIENTO, Ib, 422",
            "AMORTIGUAMIENTO, II, 422",
            "AMORTIGUAMIENTO, iii, 201",
            "AMORTIGUAMIENTO, IV, 201"})
    void categoriasSegunZona(Zona zona, String categoria, int esperado) throws Exception {
        agronomos.save(Datos.agronomo("a1", EstadoMatricula.VALIDADA, LIMITE.plusYears(1)));
        long id = ordenes.save(Datos.orden("p1", zona, LIMITE, EstadoOrden.RECETA_PENDIENTE)).getId();
        firmar(id, categoria).andExpect(status().is(esperado));
    }

    @Test
    void seUsaLaCopiaDeCategoriasTomadaAlCrearLaOrden() throws Exception {
        // la provincia permitía solo IV cuando se creó la orden: aunque hoy permita III, vale la copia
        agronomos.save(Datos.agronomo("a1", EstadoMatricula.VALIDADA, LIMITE.plusYears(1)));
        OrdenTrabajo o = Datos.orden("p1", Zona.AMORTIGUAMIENTO, LIMITE, EstadoOrden.RECETA_PENDIENTE);
        o.setCategorias(List.of("IV"));
        long id = ordenes.save(o).getId();
        firmar(id, "III").andExpect(status().isUnprocessableEntity());
        firmar(id, "IV").andExpect(status().isCreated());
    }
}
