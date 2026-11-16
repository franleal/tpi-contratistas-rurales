package ar.edu.tpi.contratistas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static ar.edu.tpi.contratistas.Usuarios.ADMIN;
import static ar.edu.tpi.contratistas.Usuarios.C1;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AdminHabilitacionControllerTest {

    @Autowired
    MockMvc mvc;

    long carnet;
    long inspeccion;

    @BeforeEach
    void setUp() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        carnet = Fixtures.habilitacion(mvc, C1, "CARNET_APLICADOR", Fixtures.aplicador(mvc, C1, "20111222"), "2027-01-01");
        inspeccion = Fixtures.habilitacion(mvc, C1, "INSPECCION_MAQUINA", Fixtures.maquina(mvc, C1), "2027-01-01");
    }

    @Test
    void listaLasPendientes() throws Exception {
        mvc.perform(get("/api/admin/habilitaciones").param("estado", "PENDIENTE").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void apruebaYDejaDeEstarPendiente() throws Exception {
        mvc.perform(post("/api/admin/habilitaciones/" + carnet + "/aprobar").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("VALIDADA"));
        mvc.perform(get("/api/admin/habilitaciones").param("estado", "PENDIENTE").with(ADMIN))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rechazaConMotivo() throws Exception {
        mvc.perform(post("/api/admin/habilitaciones/" + inspeccion + "/rechazar").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"motivo":"El certificado no corresponde a la máquina"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("El certificado no corresponde a la máquina"));
    }

    @Test
    void rechazoSinMotivoDevuelve400() throws Exception {
        mvc.perform(post("/api/admin/habilitaciones/" + inspeccion + "/rechazar").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"motivo":" "}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resolverDosVecesDevuelve409() throws Exception {
        mvc.perform(post("/api/admin/habilitaciones/" + carnet + "/aprobar").with(ADMIN)).andExpect(status().isOk());
        mvc.perform(post("/api/admin/habilitaciones/" + carnet + "/aprobar").with(ADMIN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
        mvc.perform(post("/api/admin/habilitaciones/" + carnet + "/rechazar").with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"motivo":"x"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void inexistenteDevuelve404() throws Exception {
        mvc.perform(post("/api/admin/habilitaciones/999/aprobar").with(ADMIN)).andExpect(status().isNotFound());
    }
}
