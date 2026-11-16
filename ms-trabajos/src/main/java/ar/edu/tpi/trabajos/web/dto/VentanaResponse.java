package ar.edu.tpi.trabajos.web.dto;

import java.time.LocalDateTime;

/** Tramo de horas consecutivas aptas: [desde, hasta). */
public record VentanaResponse(LocalDateTime desde, LocalDateTime hasta, int horas) {
}
