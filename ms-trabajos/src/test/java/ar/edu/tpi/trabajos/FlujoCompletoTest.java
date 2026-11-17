package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.cliente.CamposClient;
import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.dominio.Zona;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static ar.edu.tpi.trabajos.Usuarios.A1;
import static ar.edu.tpi.trabajos.Usuarios.ADMIN;
import static ar.edu.tpi.trabajos.Usuarios.C1;
import static ar.edu.tpi.trabajos.Usuarios.P1;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tarjeta 05-15: caso de uso principal de punta a punta por la API de ms-trabajos, con las
 * dependencias externas simuladas: orden → receta → ventanas → postulación → asignación →
 * ejecución → confirmación (RECETA_PENDIENTE → ABIERTA → ASIGNADA → EJECUTADA → COMPLETADA).
 * Usa data.sql real para los umbrales.
 */
@SpringBootTest(properties = "spring.sql.init.mode=always")
@AutoConfigureMockMvc
@DirtiesContext
class FlujoCompletoTest {

    static final ZoneId AR = ZoneId.of("America/Argentina/Buenos_Aires");
    static final LocalDateTime HOY = LocalDateTime.of(2026, 12, 10, 6, 0);

    /** Reloj que se puede adelantar para registrar la ejecución después del trabajo. */
    static class RelojMovil extends Clock {
        Instant ahora = HOY.atZone(AR).toInstant();

        @Override
        public ZoneId getZone() {
            return AR;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        void ir(LocalDateTime t) {
            ahora = t.atZone(AR).toInstant();
        }
    }

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        RelojMovil relojMovil() {
            return new RelojMovil();
        }
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    RelojMovil reloj;
    @MockitoBean
    CamposClient campos;
    @MockitoBean
    ContratistasClient contratistas;
    @MockitoBean
    OpenMeteoClient meteo;

    String ok(MockHttpServletRequestBuilder req, int estado) throws Exception {
        return mvc.perform(req.contentType(MediaType.APPLICATION_JSON)).andExpect(status().is(estado))
                .andReturn().getResponse().getContentAsString();
    }

    static long id(String json, String path) {
        return ((Number) JsonPath.read(json, path)).longValue();
    }

    @Test
    void ordenDePulverizacionDePuntaAPunta() throws Exception {
        when(campos.lote(1L)).thenReturn(Lotes.lote(1, "p1", Zona.AMORTIGUAMIENTO));
        LocalDateTime d = HOY.toLocalDate().atStartOfDay();
        when(meteo.horas(org.mockito.ArgumentMatchers.anyDouble(), org.mockito.ArgumentMatchers.anyDouble(), any(),
                any())).thenReturn(Climas.aptas(d.minusDays(1), d.plusDays(10)));
        when(contratistas.habilitacion(anyLong(), anyLong(), any())).thenAnswer(i -> Escenario.habilitada(3L, "c1"));
        when(contratistas.disponibilidad(anyLong(), any(), any())).thenAnswer(i ->
                new ContratistasClient.Disponibilidad(3L, i.getArgument(1), i.getArgument(2), true, null));
        when(contratistas.reservar(anyLong(), anyLong(), any(), any())).thenAnswer(i ->
                new ContratistasClient.Reserva(900L, i.getArgument(0), i.getArgument(1), i.getArgument(2),
                        i.getArgument(3), "ACTIVA"));

        // F-10: el productor crea la orden
        long orden = id(ok(post("/api/ordenes").with(P1).content("{\"loteId\":1,\"fechaLimite\":\"2026-12-15\"}"), 201),
                "$.id");

        // F-07 / F-08: el agrónomo registra la matrícula y el admin la valida
        long agronomo = id(ok(post("/api/agronomos").with(A1)
                .content("{\"matricula\":\"M-1\",\"provincia\":\"Córdoba\",\"vencimiento\":\"2027-12-31\"}"), 201), "$.id");
        ok(post("/api/admin/matriculas/" + agronomo + "/aprobar").with(ADMIN), 200);

        // F-11: receta con categoría permitida en amortiguamiento
        mvc.perform(post("/api/ordenes/" + orden + "/receta").with(A1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"producto\":\"2,4-D\",\"categoriaToxicologica\":\"IV\",\"dosis\":\"1 l/ha\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orden.estado").value("ABIERTA"));

        // F-12: hay ventanas aptas
        mvc.perform(get("/api/ordenes/" + orden + "/ventanas").with(C1))
                .andExpect(status().isOk()).andExpect(jsonPath("$", not(hasSize(0))));

        // F-13 / F-14: el contratista se postula y el productor ve la oferta
        long postulacion = id(ok(post("/api/ordenes/" + orden + "/postulaciones").with(C1)
                .content("{\"maquinaId\":3,\"aplicadorId\":4,\"inicio\":\"2026-12-11T08:00:00\",\"precioHa\":15000}"),
                201), "$.id");
        mvc.perform(get("/api/ordenes/" + orden + "/postulaciones").with(P1))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].fin").value("2026-12-11T12:00:00"));

        // F-15: asignación con reserva de agenda
        mvc.perform(post("/api/ordenes/" + orden + "/asignacion").with(P1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postulacionId\":%d}".formatted(postulacion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden.estado").value("ASIGNADA"))
                .andExpect(jsonPath("$.asignacion.reservaId").value(900));

        // F-16: al día siguiente del trabajo, el contratista registra la ejecución
        reloj.ir(LocalDateTime.of(2026, 12, 11, 18, 0));
        mvc.perform(post("/api/ordenes/" + orden + "/ejecucion").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"2026-12-11T08:10:00\",\"fin\":\"2026-12-11T12:05:00\",\"hectareas\":100}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orden.estado").value("EJECUTADA"))
                .andExpect(jsonPath("$.ejecucion.fotoClima", hasSize(5)))
                .andExpect(jsonPath("$.ejecucion.fueraDeCondiciones").value(false));

        // F-17: el productor confirma
        mvc.perform(post("/api/ordenes/" + orden + "/confirmacion").with(P1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden.estado").value("COMPLETADA"));

        // el detalle muestra todo el recorrido
        mvc.perform(get("/api/ordenes/" + orden).with(P1))
                .andExpect(jsonPath("$.receta.categoriaToxicologica").value("IV"))
                .andExpect(jsonPath("$.asignacion.postulacionId").value(postulacion))
                .andExpect(jsonPath("$.ejecucion.confirmadaEn").value("2026-12-11T18:00:00"));
    }
}
