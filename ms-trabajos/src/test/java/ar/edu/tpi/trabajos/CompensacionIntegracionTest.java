package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.AsignacionRepository;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.PostulacionRepository;
import ar.edu.tpi.trabajos.repositorio.UmbralClimaticoRepository;
import ar.edu.tpi.trabajos.servicio.AsignacionPersistencia;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static ar.edu.tpi.trabajos.Usuarios.C1;
import static ar.edu.tpi.trabajos.Usuarios.P1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tarjeta 05-13 (F-15, D-07): con el contexto completo, si falla el guardado local después de
 * reservar, se libera la reserva y la orden queda como estaba (ABIERTA, postulación VALIDA).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojFijo.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CompensacionIntegracionTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @Autowired
    PostulacionRepository postulaciones;
    @Autowired
    AsignacionRepository asignaciones;
    @Autowired
    UmbralClimaticoRepository umbrales;
    @MockitoBean
    ContratistasClient contratistas;
    @MockitoBean
    OpenMeteoClient meteo;
    @MockitoSpyBean
    AsignacionPersistencia persistencia;

    long orden;
    long postulacion;

    @BeforeEach
    void setUp() throws Exception {
        umbrales.save(Climas.umbral());
        orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 14), EstadoOrden.ABIERTA)).getId();
        Escenario.apto(contratistas, meteo);
        String body = mvc.perform(post("/api/ordenes/" + orden + "/postulaciones").with(C1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maquinaId\":3,\"aplicadorId\":4,\"inicio\":\"%s\",\"precioHa\":100}"
                                .formatted(Escenario.INICIO)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        postulacion = ((Number) JsonPath.read(body, "$.id")).longValue();
        when(contratistas.reservar(anyLong(), anyLong(), any(), any()))
                .thenAnswer(i -> new ContratistasClient.Reserva(77L, i.getArgument(0), i.getArgument(1),
                        i.getArgument(2), i.getArgument(3), "ACTIVA"));
    }

    @Test
    void falloAlGuardarLiberaLaReservaYNoCambiaNada() throws Exception {
        doThrow(new DataAccessResourceFailureException("conexión perdida"))
                .when(persistencia).guardar(anyLong(), anyLong(), anyLong());

        mvc.perform(post("/api/ordenes/" + orden + "/asignacion").with(P1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postulacionId\":%d}".formatted(postulacion)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("DEPENDENCIA_NO_DISPONIBLE"));

        verify(contratistas).liberar(77L);
        assertThat(ordenes.findById(orden).orElseThrow().getEstado()).isEqualTo(EstadoOrden.ABIERTA);
        assertThat(postulaciones.findById(postulacion).orElseThrow().getEstado()).isEqualTo(EstadoPostulacion.VALIDA);
        assertThat(asignaciones.count()).isZero();
    }

    @Test
    void despuesDeCompensarSePuedeVolverAAsignar() throws Exception {
        doThrow(new DataAccessResourceFailureException("conexión perdida"))
                .doCallRealMethod()
                .when(persistencia).guardar(anyLong(), anyLong(), anyLong());
        String body = "{\"postulacionId\":%d}".formatted(postulacion);

        mvc.perform(post("/api/ordenes/" + orden + "/asignacion").with(P1).contentType(MediaType.APPLICATION_JSON)
                .content(body)).andExpect(status().isServiceUnavailable());
        mvc.perform(post("/api/ordenes/" + orden + "/asignacion").with(P1).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden.estado").value("ASIGNADA"));
    }
}
