package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient.HoraClima;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static ar.edu.tpi.trabajos.Usuarios.C1;
import static ar.edu.tpi.trabajos.Usuarios.C2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojFijo.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PostulacionControllerTest {

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

    @BeforeEach
    void setUp() {
        umbrales.save(Climas.umbral());
        orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 14), EstadoOrden.ABIERTA)).getId();
        Escenario.apto(contratistas, meteo);
    }

    ResultActions postularse(RequestPostProcessor quien, LocalDateTime inicio) throws Exception {
        return mvc.perform(post("/api/ordenes/" + orden + "/postulaciones").with(quien)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"maquinaId\":3,\"aplicadorId\":4,\"inicio\":\"%s\",\"precioHa\":12500}".formatted(inicio)));
    }

    @Test
    void postulacionValidaConHorasYTraslado() throws Exception {
        postularse(C1, Escenario.INICIO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("VALIDA"))
                .andExpect(jsonPath("$.horasAplicacion").value(4.0)) // 100 ha / 25 ha/h
                .andExpect(jsonPath("$.fin").value("2026-12-11T12:00:00"))
                .andExpect(jsonPath("$.trasladoH").value(0.0))
                .andExpect(jsonPath("$.contratistaSub").value("c1"));
        verify(contratistas).disponibilidad(3L, Escenario.INICIO, Escenario.INICIO.plusHours(4));
    }

    @Test
    void maquinaAjenaDevuelve403() throws Exception {
        postularse(C2, Escenario.INICIO).andExpect(status().isForbidden());
        assertThat(postulaciones.count()).isZero();
    }

    @Test
    void habilitacionVencidaDevuelve422ConElMotivo() throws Exception {
        when(contratistas.habilitacion(anyLong(), anyLong(), any(LocalDate.class)))
                .thenReturn(new ContratistasClient.Habilitacion(3L, 4L, Escenario.INICIO.toLocalDate(), false,
                        "El carnet del aplicador 4 vence el 2026-12-10 (RN-02, RN-04)", "PULVERIZADORA",
                        java.math.BigDecimal.TEN, java.math.BigDecimal.ONE, java.math.BigDecimal.ONE, "c1"));
        postularse(C1, Escenario.INICIO).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("RN-02")));
    }

    @Test
    void inicioEnElPasadoDevuelve422() throws Exception {
        postularse(C1, RelojFijo.AHORA.minusHours(1)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("RN-10")));
    }

    @Test
    void fueraDelHorizonteDevuelve422() throws Exception {
        LocalDate lejos = LocalDate.of(2026, 12, 31);
        orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, lejos, EstadoOrden.ABIERTA)).getId();
        postularse(C1, RelojFijo.AHORA.plusDays(7)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("horizonte")));
    }

    @Test
    void posteriorALaFechaLimiteDevuelve422() throws Exception {
        postularse(C1, LocalDateTime.of(2026, 12, 14, 22, 0)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("fecha límite")));
    }

    @Test
    void agendaOcupadaDevuelve422() throws Exception {
        when(contratistas.disponibilidad(anyLong(), any(), any())).thenReturn(new ContratistasClient.Disponibilidad(
                3L, Escenario.INICIO, Escenario.INICIO.plusHours(4), false, "ya tiene una reserva (RN-13)"));
        postularse(C1, Escenario.INICIO).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString("RN-13")));
    }

    @Test
    void climaNoAptoDevuelve422ConLaPrimeraHoraQueFalla() throws Exception {
        LocalDateTime d = RelojFijo.AHORA.toLocalDate().atStartOfDay();
        var p = Climas.aptas(d, d.plusDays(10));
        LocalDateTime h = Escenario.INICIO.plusHours(2);
        p.put(h, new HoraClima(h, 22, 30, 0, 25, 60));
        when(meteo.horas(anyDouble(), anyDouble(), any(), any())).thenReturn(p);
        postularse(C1, Escenario.INICIO).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje", containsString(h.toString())));
        assertThat(postulaciones.count()).isZero();
    }

    @Test
    void openMeteoCaidoDevuelve503SinGuardar() throws Exception {
        when(meteo.horas(anyDouble(), anyDouble(), any(), any()))
                .thenThrow(new DependenciaNoDisponibleException("Open-Meteo no está disponible"));
        postularse(C1, Escenario.INICIO).andExpect(status().isServiceUnavailable());
        assertThat(postulaciones.count()).isZero();
    }

    @Test
    void ordenNoAbiertaDevuelve409() throws Exception {
        orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 14),
                EstadoOrden.RECETA_PENDIENTE)).getId();
        postularse(C1, Escenario.INICIO).andExpect(status().isConflict());
    }
}
