package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import ar.edu.tpi.trabajos.dominio.Asignacion;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.dominio.Postulacion;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.AsignacionRepository;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.PostulacionRepository;
import ar.edu.tpi.trabajos.repositorio.RegistroEjecucionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static ar.edu.tpi.trabajos.Usuarios.C1;
import static ar.edu.tpi.trabajos.Usuarios.P1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tarjeta 05-14 (RN-09, RN-16, falla segura): con los clientes HTTP reales apuntando a un puerto
 * cerrado (como los overrides docker-compose.falla-*.yml), las operaciones que dependen del
 * pronóstico responden 503 sin guardar nada, y la ejecución se registra igual sin foto climática.
 * ms-contratistas se simula para poder llegar hasta la consulta del clima.
 */
@SpringBootTest(properties = {
        "open-meteo.url=http://127.0.0.1:9",
        "services.campos.url=http://127.0.0.1:9",
        "http.connect-timeout-ms=300",
        "http.read-timeout-ms=300"})
@AutoConfigureMockMvc
@Import(RelojFijo.class)
@DirtiesContext
class FallaSeguraTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @Autowired
    PostulacionRepository postulaciones;
    @Autowired
    RegistroEjecucionRepository ejecuciones;
    @Autowired
    UmbralClimaticoRepository umbrales;
    @Autowired
    AsignacionRepository asignaciones;
    @MockitoBean
    ContratistasClient contratistas;

    @BeforeEach
    void setUp() {
        umbrales.save(Climas.umbral());
        when(contratistas.habilitacion(anyLong(), anyLong(), any())).thenAnswer(i -> Escenario.habilitada(3L, "c1"));
        when(contratistas.disponibilidad(anyLong(), any(), any())).thenAnswer(i ->
                new ContratistasClient.Disponibilidad(3L, i.getArgument(1), i.getArgument(2), true, null));
    }

    @Test
    void camposCaidoAlCrearLaOrdenDevuelve503SinGuardar() throws Exception {
        long antes = ordenes.count();
        mvc.perform(post("/api/ordenes").with(P1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loteId\":1,\"fechaLimite\":\"2026-12-20\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("DEPENDENCIA_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.correlationId").exists());
        assertThat(ordenes.count()).isEqualTo(antes);
    }

    @Test
    void openMeteoCaidoAlPostularseDevuelve503SinGuardar() throws Exception {
        long orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 14), EstadoOrden.ABIERTA))
                .getId();
        long antes = postulaciones.count();
        mvc.perform(post("/api/ordenes/" + orden + "/postulaciones").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maquinaId\":3,\"aplicadorId\":4,\"inicio\":\"%s\",\"precioHa\":100}"
                                .formatted(Escenario.INICIO)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.mensaje").value("Open-Meteo no está disponible"));
        assertThat(postulaciones.count()).isEqualTo(antes);
    }

    @Test
    void openMeteoCaidoAlConsultarVentanasDevuelve503() throws Exception {
        long orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 14), EstadoOrden.ABIERTA))
                .getId();
        mvc.perform(get("/api/ordenes/" + orden + "/ventanas").with(P1)).andExpect(status().isServiceUnavailable());
    }

    @Test
    void openMeteoCaidoAlRegistrarLaEjecucionLaRegistraSinFoto() throws Exception {
        long orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 14), EstadoOrden.ASIGNADA))
                .getId();
        var o = ordenes.findById(orden).orElseThrow();
        Postulacion p = new Postulacion();
        p.setOrden(o);
        p.setContratistaSub("c1");
        p.setMaquinaId(3L);
        p.setAplicadorId(4L);
        LocalDateTime inicio = RelojFijo.AHORA.minusHours(5);
        p.setInicio(inicio);
        p.setFin(inicio.plusHours(4));
        p.setOcupadoDesde(inicio);
        p.setHorasAplicacion(new BigDecimal("4"));
        p.setTrasladoH(BigDecimal.ZERO);
        p.setPrecioHa(BigDecimal.TEN);
        p.setEstado(EstadoPostulacion.ASIGNADA);
        postulaciones.save(p);
        Asignacion a = new Asignacion();
        a.setOrden(o);
        a.setPostulacion(p);
        a.setReservaId(1L);
        asignaciones.save(a);

        mvc.perform(post("/api/ordenes/" + orden + "/ejecucion").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"%s\",\"fin\":\"%s\",\"hectareas\":90}".formatted(inicio,
                                inicio.plusHours(4))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orden.estado").value("EJECUTADA"))
                .andExpect(jsonPath("$.ejecucion.climaDisponible").value(false));
        assertThat(ejecuciones.findByOrdenId(orden)).isPresent();
    }
}
