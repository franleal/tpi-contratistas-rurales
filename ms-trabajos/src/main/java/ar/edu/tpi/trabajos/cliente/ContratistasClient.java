package ar.edu.tpi.trabajos.cliente;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Cliente de las rutas /internal de ms-contratistas (docs/CONTRATOS.md). */
@Component
public class ContratistasClient {

    static final String SERVICIO = "ms-contratistas";

    private final RestClient http;

    public ContratistasClient(@Qualifier("contratistasRestClient") RestClient http) {
        this.http = http;
    }

    public record Habilitacion(Long maquinaId, Long aplicadorId, LocalDate fecha, boolean habilitado, String motivo,
                               String tipoMaquina, BigDecimal rendimientoHaH, BigDecimal baseLat,
                               BigDecimal baseLon, String contratistaSub) {
    }

    public record Disponibilidad(Long maquinaId, LocalDateTime desde, LocalDateTime hasta, boolean disponible,
                                 String motivo) {
    }

    public record SolicitudReserva(Long maquinaId, Long ordenId, LocalDateTime desde, LocalDateTime hasta) {
    }

    public record Reserva(Long id, Long maquinaId, Long ordenId, LocalDateTime desde, LocalDateTime hasta,
                          String estado) {
    }

    /** RN-02 a RN-04 evaluadas por ms-contratistas a la fecha del trabajo. */
    public Habilitacion habilitacion(Long maquinaId, Long aplicadorId, LocalDate fecha) {
        return ErroresInternos.llamar(SERVICIO, () -> http.get()
                .uri(u -> u.path("/internal/maquinas/{id}/habilitacion")
                        .queryParam("aplicadorId", aplicadorId).queryParam("fecha", fecha).build(maquinaId))
                .retrieve().body(Habilitacion.class));
    }

    /** RN-13: consulta sin efectos. */
    public Disponibilidad disponibilidad(Long maquinaId, LocalDateTime desde, LocalDateTime hasta) {
        return ErroresInternos.llamar(SERVICIO, () -> http.get()
                .uri(u -> u.path("/internal/maquinas/{id}/disponibilidad")
                        .queryParam("desde", desde).queryParam("hasta", hasta).build(maquinaId))
                .retrieve().body(Disponibilidad.class));
    }

    /** RN-13 al asignar: 409 si el intervalo se tomó mientras tanto, 422 si quedó fuera de un bloque. */
    public Reserva reservar(Long maquinaId, Long ordenId, LocalDateTime desde, LocalDateTime hasta) {
        return ErroresInternos.llamar(SERVICIO, () -> http.post().uri("/internal/agenda/reservas")
                .body(new SolicitudReserva(maquinaId, ordenId, desde, hasta))
                .retrieve().body(Reserva.class));
    }

    /** Compensación: libera la reserva (idempotente en ms-contratistas). */
    public void liberar(Long reservaId) {
        ErroresInternos.ejecutar(SERVICIO,
                () -> http.delete().uri("/internal/agenda/reservas/{id}", reservaId).retrieve().toBodilessEntity());
    }
}
