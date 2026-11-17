package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.trabajos.cliente.CamposClient;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static ar.edu.tpi.trabajos.Usuarios.P1;
import static ar.edu.tpi.trabajos.Usuarios.P2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrdenControllerTest {

    static final String LIMITE = LocalDate.now().plusDays(10).toString();

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @MockitoBean
    CamposClient campos;

    ResultActions crear(org.springframework.test.web.servlet.request.RequestPostProcessor quien, long lote)
            throws Exception {
        return mvc.perform(post("/api/ordenes").with(quien).contentType(MediaType.APPLICATION_JSON)
                .content("{\"loteId\":%d,\"fechaLimite\":\"%s\"}".formatted(lote, LIMITE)));
    }

    @Test
    void creaLaOrdenEnRecetaPendienteConCopiaDelLote() throws Exception {
        when(campos.lote(1L)).thenReturn(Lotes.lote(1, "p1", Zona.AMORTIGUAMIENTO));
        crear(P1, 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECETA_PENDIENTE"))
                .andExpect(jsonPath("$.zona").value("AMORTIGUAMIENTO"))
                .andExpect(jsonPath("$.superficieHa").value(100.0))
                .andExpect(jsonPath("$.categoriasPermitidas[1]").value("IV"));
    }

    @Test
    void loteEnExclusionDevuelve422YNoCreaLaOrden() throws Exception {
        when(campos.lote(1L)).thenReturn(Lotes.lote(1, "p1", Zona.EXCLUSION));
        crear(P1, 1)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("RN-06")));
        assertThat(ordenes.count()).isZero();
    }

    @Test
    void loteAjenoDevuelve403() throws Exception {
        when(campos.lote(1L)).thenReturn(Lotes.lote(1, "p1", Zona.LIBRE));
        crear(P2, 1).andExpect(status().isForbidden());
    }

    @Test
    void loteInexistenteDevuelve404() throws Exception {
        when(campos.lote(9L)).thenThrow(new RecursoNoEncontradoException("No existe el lote 9"));
        crear(P1, 9).andExpect(status().isNotFound());
    }

    @Test
    void camposCaidoDevuelve503SinGuardar() throws Exception {
        when(campos.lote(1L)).thenThrow(new DependenciaNoDisponibleException("ms-campos no está disponible"));
        crear(P1, 1).andExpect(status().isServiceUnavailable());
        assertThat(ordenes.count()).isZero();
    }

    @Test
    void fechaLimitePasadaDevuelve400() throws Exception {
        mvc.perform(post("/api/ordenes").with(P1).contentType(MediaType.APPLICATION_JSON)
                .content("{\"loteId\":1,\"fechaLimite\":\"2020-01-01\"}")).andExpect(status().isBadRequest());
    }
}
