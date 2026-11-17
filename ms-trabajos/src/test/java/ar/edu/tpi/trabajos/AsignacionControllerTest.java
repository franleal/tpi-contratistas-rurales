package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.PostulacionRepository;
import ar.edu.tpi.trabajos.repositorio.UmbralClimaticoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static ar.edu.tpi.trabajos.Usuarios.C1;
import static ar.edu.tpi.trabajos.Usuarios.C2;
import static ar.edu.tpi.trabajos.Usuarios.P1;
import static ar.edu.tpi.trabajos.Usuarios.P2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojFijo.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AsignacionControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @Autowired
    PostulacionRepository postulaciones;
    @Autowired
    UmbralClimaticoRepository umbrales;
    @MockitoBean
    ContratistasClient contratistas;
    @MockitoBean
    OpenMeteoClient meteo;

    long orden;
    long primera;
    long segunda;

    long postularse(RequestPostProcessor quien, LocalDateTime inicio) throws Exception {
        String body = mvc.perform(post("/api/ordenes/" + orden + "/postulaciones").with(quien)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maquinaId\":3,\"aplicadorId\":4,\"inicio\":\"%s\",\"precioHa\":100}"
                                .formatted(inicio)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();
    }

    @BeforeEach
    void setUp() throws Exception {
        umbrales.save(Climas.umbral());
        orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 14), EstadoOrden.ABIERTA)).getId();
        Escenario.apto(contratistas, meteo);
        primera = postularse(C1, Escenario.INICIO);
        Escenario.maquinaDe(contratistas, 3L, "c2");
        segunda = postularse(C2, Escenario.INICIO.plusDays(1));
        Escenario.apto(contratistas, meteo);
        when(contratistas.reservar(anyLong(), anyLong(), any(), any()))
                .thenAnswer(i -> new ContratistasClient.Reserva(50L, i.getArgument(0), i.getArgument(1),
                        i.getArgument(2), i.getArgument(3), "ACTIVA"));
    }

    ResultActions asignar(RequestPostProcessor quien, long postulacion) throws Exception {
        return mvc.perform(post("/api/ordenes/" + orden + "/asignacion").with(quien)
                .contentType(MediaType.APPLICATION_JSON).content("{\"postulacionId\":%d}".formatted(postulacion)));
    }

    @Test
    void asignaReservaYDescartaLasDemas() throws Exception {
        asignar(P1, primera)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden.estado").value("ASIGNADA"))
                .andExpect(jsonPath("$.asignacion.postulacionId").value(primera))
                .andExpect(jsonPath("$.asignacion.reservaId").value(50))
                .andExpect(jsonPath("$.asignacion.contratistaSub").value("c1"));
        verify(contratistas).reservar(3L, orden, Escenario.INICIO, Escenario.INICIO.plusHours(4));
        assertThat(postulaciones.findById(primera).orElseThrow().getEstado()).isEqualTo(EstadoPostulacion.ASIGNADA);
        assertThat(postulaciones.findById(segunda).orElseThrow().getEstado()).isEqualTo(EstadoPostulacion.DESCARTADA);
        // el contratista asignado ve la orden; el otro solo ve abiertas
        mvc.perform(get("/api/ordenes").with(C1)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/ordenes").with(C2)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void bloqueTomadoMientrasTantoDevuelve409YLaOrdenSigueAbierta() throws Exception {
        when(contratistas.reservar(anyLong(), anyLong(), any(), any()))
                .thenThrow(new ConflictoEstadoException("La máquina 3 ya tiene una reserva (RN-13)"));
        asignar(P1, primera).andExpect(status().isConflict());
        assertThat(ordenes.findById(orden).orElseThrow().getEstado()).isEqualTo(EstadoOrden.ABIERTA);
        assertThat(postulaciones.findById(primera).orElseThrow().getEstado()).isEqualTo(EstadoPostulacion.VALIDA);
    }

    @Test
    void habilitacionVencidaAlAsignarDevuelve422SinReservar() throws Exception {
        when(contratistas.habilitacion(anyLong(), anyLong(), any())).thenReturn(new ContratistasClient.Habilitacion(
                3L, 4L, Escenario.INICIO.toLocalDate(), false, "La inspección fue RECHAZADA (RN-03, RN-04)",
                "PULVERIZADORA", BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, "c1"));
        asignar(P1, primera).andExpect(status().isUnprocessableEntity());
        verify(contratistas, never()).reservar(anyLong(), anyLong(), any(), any());
    }

    @Test
    void ordenAjenaDevuelve403() throws Exception {
        asignar(P2, primera).andExpect(status().isForbidden());
    }

    @Test
    void postulacionDeOtraOrdenDevuelve404() throws Exception {
        asignar(P1, 999).andExpect(status().isNotFound());
    }

    @Test
    void segundaAsignacionDevuelve409() throws Exception {
        asignar(P1, primera).andExpect(status().isOk());
        asignar(P1, segunda).andExpect(status().isConflict());
    }
}
