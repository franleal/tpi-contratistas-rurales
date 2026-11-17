package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient.HoraClima;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.UmbralClimaticoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static ar.edu.tpi.trabajos.Usuarios.C1;
import static ar.edu.tpi.trabajos.Usuarios.P1;
import static ar.edu.tpi.trabajos.Usuarios.P2;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VentanaControllerTest {

    /** "Ahora" fijo: 10/12/2026 05:20 en Argentina. */
    static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 10, 5, 20);

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            ZoneId ar = ZoneId.of("America/Argentina/Buenos_Aires");
            return Clock.fixed(AHORA.atZone(ar).toInstant(), ar);
        }
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @Autowired
    UmbralClimaticoRepository umbrales;
    @MockitoBean
    OpenMeteoClient meteo;

    long orden;

    @BeforeEach
    void setUp() {
        umbrales.save(Climas.umbral());
        // fecha límite al día siguiente: ventanas desde las 06:00 del 10 hasta las 00:00 del 12
        orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 11), EstadoOrden.ABIERTA)).getId();
    }

    void pronostico(Map<LocalDateTime, HoraClima> horas) {
        when(meteo.horas(anyDouble(), anyDouble(), any(), any())).thenReturn(horas);
    }

    @Test
    void devuelveLosTramosAptos() throws Exception {
        LocalDateTime d10 = LocalDateTime.of(2026, 12, 10, 0, 0);
        Map<LocalDateTime, HoraClima> p = Climas.aptas(d10, d10.plusDays(3));
        LocalDateTime ventoso = d10.withHour(12);
        p.put(ventoso, new HoraClima(ventoso, 30, 40, 0, 25, 60));
        pronostico(p);

        mvc.perform(get("/api/ordenes/" + orden + "/ventanas").with(P1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].desde").value("2026-12-10T06:00:00"))
                .andExpect(jsonPath("$[0].hasta").value("2026-12-10T12:00:00"))
                .andExpect(jsonPath("$[0].horas").value(6))
                .andExpect(jsonPath("$[1].desde").value("2026-12-10T13:00:00"))
                .andExpect(jsonPath("$[1].hasta").value("2026-12-12T00:00:00"));
    }

    @Test
    void lluviaCortaLasHorasPreviasDentroDeN() throws Exception {
        LocalDateTime d10 = LocalDateTime.of(2026, 12, 10, 0, 0);
        Map<LocalDateTime, HoraClima> p = Climas.aptas(d10, d10.plusDays(3));
        LocalDateTime lluvia = d10.withHour(10);
        p.put(lluvia, new HoraClima(lluvia, 10, 15, 2, 22, 80));
        pronostico(p);
        // con N = 3, las 07, 08, 09 y 10 quedan afuera
        mvc.perform(get("/api/ordenes/" + orden + "/ventanas").with(C1))
                .andExpect(jsonPath("$[0].hasta").value("2026-12-10T07:00:00"))
                .andExpect(jsonPath("$[1].desde").value("2026-12-10T11:00:00"));
    }

    @Test
    void ordenAjenaDevuelve403() throws Exception {
        mvc.perform(get("/api/ordenes/" + orden + "/ventanas").with(P2)).andExpect(status().isForbidden());
    }

    @Test
    void ordenSinRecetaDevuelve409() throws Exception {
        long pendiente = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 11),
                EstadoOrden.RECETA_PENDIENTE)).getId();
        mvc.perform(get("/api/ordenes/" + pendiente + "/ventanas").with(P1)).andExpect(status().isConflict());
    }

    @Test
    void openMeteoCaidoDevuelve503() throws Exception {
        when(meteo.horas(anyDouble(), anyDouble(), any(), any()))
                .thenThrow(new DependenciaNoDisponibleException("Open-Meteo no está disponible"));
        mvc.perform(get("/api/ordenes/" + orden + "/ventanas").with(P1))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("DEPENDENCIA_NO_DISPONIBLE"));
    }
}
