package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient.HoraClima;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import static ar.edu.tpi.trabajos.Usuarios.C1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RN-16: foto climática por hora trabajada. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojFijo.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FotoClimaTest extends EjecucionBase {

    @BeforeEach
    void setUp() {
        orden = ordenAsignada("p1", "c1");
    }

    @Test
    void guardaUnaFotoPorHoraYMarcaLasFueraDeUmbral() throws Exception {
        LocalDateTime d = INICIO.toLocalDate().atStartOfDay();
        var clima = Climas.aptas(d, d.plusDays(1));
        LocalDateTime h = INICIO.plusHours(2);
        clima.put(h, new HoraClima(h, 18, 25, 0, 24, 60));
        when(meteo.horas(anyDouble(), anyDouble(), any(), any())).thenReturn(clima);

        registrar(C1, INICIO.plusMinutes(15), INICIO.plusHours(3).plusMinutes(30), "80")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ejecucion.climaDisponible").value(true))
                .andExpect(jsonPath("$.ejecucion.fueraDeCondiciones").value(true))
                .andExpect(jsonPath("$.ejecucion.fotoClima", hasSize(4))) // 08, 09, 10 y 11
                .andExpect(jsonPath("$.ejecucion.fotoClima[2].apta").value(false))
                .andExpect(jsonPath("$.ejecucion.fotoClima[2].motivo", containsString("viento")))
                .andExpect(jsonPath("$.ejecucion.fotoClima[0].apta").value(true));
    }

    /**
     * Trabajo de 08:00 a 10:00 con lluvia sólo de 09:00 a 10:00. Open-Meteo la informa en la marca
     * 10:00 (hora anterior): la foto de las 09:00 debe reflejarla (RN-16).
     */
    @Test
    void laFotoReflejaLaLluviaInformadaEnLaMarcaSiguiente() throws Exception {
        RestClient.Builder b = RestClient.builder().baseUrl("http://meteo");
        MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
        server.expect(requestTo(containsString("/v1/forecast"))).andRespond(withSuccess(Climas.openMeteo(
                INICIO.toString(), new double[]{0, 0, 2.5, 0}, null), MediaType.APPLICATION_JSON));
        var clima = new OpenMeteoClient(b.build()).horas(0, 0, INICIO.toLocalDate(), INICIO.toLocalDate());
        when(meteo.horas(anyDouble(), anyDouble(), any(), any())).thenReturn(clima);

        registrar(C1, INICIO, INICIO.plusHours(2), "50")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ejecucion.fueraDeCondiciones").value(true))
                .andExpect(jsonPath("$.ejecucion.fotoClima", hasSize(2)))
                .andExpect(jsonPath("$.ejecucion.fotoClima[0].precipitacionMm").value(0.0))
                .andExpect(jsonPath("$.ejecucion.fotoClima[0].apta").value(true))
                .andExpect(jsonPath("$.ejecucion.fotoClima[1].precipitacionMm").value(2.5))
                .andExpect(jsonPath("$.ejecucion.fotoClima[1].apta").value(false))
                .andExpect(jsonPath("$.ejecucion.fotoClima[1].motivo", containsString("lluvia")));
    }

    @Test
    void todasLasHorasAptasNoQuedaFueraDeCondiciones() throws Exception {
        LocalDateTime d = INICIO.toLocalDate().atStartOfDay();
        when(meteo.horas(anyDouble(), anyDouble(), any(), any())).thenReturn(Climas.aptas(d, d.plusDays(1)));
        registrar(C1, INICIO, INICIO.plusHours(4), "100")
                .andExpect(jsonPath("$.ejecucion.fueraDeCondiciones").value(false))
                .andExpect(jsonPath("$.ejecucion.fotoClima", hasSize(4)));
    }

    @Test
    void openMeteoCaidoRegistraIgualSinFoto() throws Exception {
        when(meteo.horas(anyDouble(), anyDouble(), any(), any()))
                .thenThrow(new DependenciaNoDisponibleException("Open-Meteo no está disponible"));
        registrar(C1, INICIO, INICIO.plusHours(4), "100")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orden.estado").value("EJECUTADA"))
                .andExpect(jsonPath("$.ejecucion.climaDisponible").value(false))
                .andExpect(jsonPath("$.ejecucion.fotoClima", hasSize(0)));
        assertThat(ordenes.findById(orden).orElseThrow().getEstado()).isEqualTo(EstadoOrden.EJECUTADA);
    }
}
