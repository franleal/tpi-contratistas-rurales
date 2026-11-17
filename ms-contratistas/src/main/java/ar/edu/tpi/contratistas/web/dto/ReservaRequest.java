package ar.edu.tpi.contratistas.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Contrato interno POST /internal/agenda/reservas. El intervalo incluye el traslado. */
public record ReservaRequest(@NotNull Long maquinaId, @NotNull Long ordenId, @NotNull LocalDateTime desde,
                             @NotNull LocalDateTime hasta) {
}
