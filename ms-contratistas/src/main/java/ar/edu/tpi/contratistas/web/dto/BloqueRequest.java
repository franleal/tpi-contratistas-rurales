package ar.edu.tpi.contratistas.web.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** F-06. Horas locales de Argentina, sin zona (por ejemplo 2026-12-10T08:00). */
public record BloqueRequest(@NotNull Long maquinaId, @NotNull LocalDateTime desde, @NotNull LocalDateTime hasta) {
}
