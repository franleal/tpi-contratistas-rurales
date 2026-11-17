package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.dominio.EstadoMatricula;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.AgronomoRepository;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.RecetaRepository;
import org.junit.jupiter.api.Test;
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

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RecetaControllerTest {

    static final LocalDate LIMITE = LocalDate.now().plusDays(10);

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @Autowired
    AgronomoRepository agronomos;
    @Autowired
    RecetaRepository recetas;

    long orden(Zona zona) {
        return ordenes.save(Datos.orden("p1", zona, LIMITE, EstadoOrden.RECETA_PENDIENTE)).getId();
    }

    void agronomo(EstadoMatricula estado, LocalDate vence) {
        agronomos.save(Datos.agronomo("a1", estado, vence));
    }

    ResultActions firmar(long orden, String categoria) throws Exception {
        return mvc.perform(post("/api/ordenes/" + orden + "/receta").with(A1).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"producto":"Glifosato 66%%","categoriaToxicologica":"%s","dosis":"2 l/ha"}""".formatted(categoria)));
    }

    @Test
    void firmaYLaOrdenPasaAAbierta() throws Exception {
        agronomo(EstadoMatricula.VALIDADA, LIMITE.plusYears(1));
        long id = orden(Zona.LIBRE);
        firmar(id, "ii")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orden.estado").value("ABIERTA"))
                .andExpect(jsonPath("$.receta.categoriaToxicologica").value("II"))
                .andExpect(jsonPath("$.receta.matricula").value("MAT-a1"));
    }

    @Test
    void sinMatriculaDevuelve403() throws Exception {
        firmar(orden(Zona.LIBRE), "IV").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje", containsString("RN-01")));
    }

    @Test
    void matriculaPendienteDevuelve403() throws Exception {
        agronomo(EstadoMatricula.PENDIENTE, LIMITE.plusYears(1));
        firmar(orden(Zona.LIBRE), "IV").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje", containsString("PENDIENTE")));
    }

    @Test
    void matriculaQueVenceAntesDeLaFechaLimiteDevuelve422() throws Exception {
        agronomo(EstadoMatricula.VALIDADA, LIMITE.minusDays(1));
        long id = orden(Zona.LIBRE);
        firmar(id, "IV").andExpect(status().isUnprocessableEntity());
        assertThat(recetas.count()).isZero();
        assertThat(ordenes.findById(id).orElseThrow().getEstado()).isEqualTo(EstadoOrden.RECETA_PENDIENTE);
    }

    @Test
    void categoriaNoPermitidaEnAmortiguamientoDevuelve422() throws Exception {
        agronomo(EstadoMatricula.VALIDADA, LIMITE.plusYears(1));
        firmar(orden(Zona.AMORTIGUAMIENTO), "II").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("RN-07")));
    }

    @Test
    void categoriaPermitidaEnAmortiguamientoSeAcepta() throws Exception {
        agronomo(EstadoMatricula.VALIDADA, LIMITE.plusYears(1));
        firmar(orden(Zona.AMORTIGUAMIENTO), "IV").andExpect(status().isCreated());
    }

    @Test
    void categoriaInexistenteDevuelve400() throws Exception {
        agronomo(EstadoMatricula.VALIDADA, LIMITE.plusYears(1));
        firmar(orden(Zona.LIBRE), "V").andExpect(status().isBadRequest());
    }
}
