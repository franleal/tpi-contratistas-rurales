package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ContratistasClientTest {

    static final LocalDateTime D8 = LocalDateTime.of(2026, 12, 10, 8, 0);

    MockRestServiceServer server;
    ContratistasClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder b = RestClient.builder().baseUrl("http://co");
        server = MockRestServiceServer.bindTo(b).build();
        cliente = new ContratistasClient(b.build());
    }

    @Test
    void consultaHabilitacion() {
        server.expect(requestTo("http://co/internal/maquinas/3/habilitacion?aplicadorId=4&fecha=2026-12-10"))
                .andRespond(withSuccess("""
                        {"maquinaId":3,"aplicadorId":4,"fecha":"2026-12-10","habilitado":true,"motivo":null,
                         "tipoMaquina":"PULVERIZADORA","rendimientoHaH":25.00,"baseLat":-31.4,"baseLon":-64.18,
                         "contratistaSub":"c1"}""", MediaType.APPLICATION_JSON));
        var h = cliente.habilitacion(3L, 4L, LocalDate.of(2026, 12, 10));
        assertThat(h.habilitado()).isTrue();
        assertThat(h.contratistaSub()).isEqualTo("c1");
    }

    @Test
    void consultaDisponibilidad() {
        server.expect(requestTo("http://co/internal/maquinas/3/disponibilidad?desde=2026-12-10T08:00&hasta=2026-12-10T12:00"))
                .andRespond(withSuccess("""
                        {"maquinaId":3,"desde":"2026-12-10T08:00:00","hasta":"2026-12-10T12:00:00","disponible":false,
                         "motivo":"fuera de bloque"}""", MediaType.APPLICATION_JSON));
        assertThat(cliente.disponibilidad(3L, D8, D8.plusHours(4)).disponible()).isFalse();
    }

    @Test
    void reservaYLibera() {
        server.expect(requestTo("http://co/internal/agenda/reservas")).andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"maquinaId\":3,\"ordenId\":7}"))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"id":11,"maquinaId":3,"ordenId":7,"desde":"2026-12-10T08:00:00","hasta":"2026-12-10T12:00:00",
                         "estado":"ACTIVA"}"""));
        server.expect(requestTo("http://co/internal/agenda/reservas/11")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());
        assertThat(cliente.reservar(3L, 7L, D8, D8.plusHours(4)).id()).isEqualTo(11L);
        cliente.liberar(11L);
        server.verify();
    }

    @Test
    void reservaTomadaSePropagaComo409() {
        server.expect(requestTo("http://co/internal/agenda/reservas"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"codigo\":\"CONFLICTO\",\"mensaje\":\"La máquina 3 ya tiene una reserva\"}"));
        assertThatThrownBy(() -> cliente.reservar(3L, 7L, D8, D8.plusHours(4)))
                .isInstanceOf(ConflictoEstadoException.class).hasMessageContaining("ya tiene una reserva");
    }

    @Test
    void servicioCaidoEs503() {
        server.expect(requestTo("http://co/internal/agenda/reservas/11"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertThatThrownBy(() -> cliente.liberar(11L)).isInstanceOf(DependenciaNoDisponibleException.class);
    }
}
