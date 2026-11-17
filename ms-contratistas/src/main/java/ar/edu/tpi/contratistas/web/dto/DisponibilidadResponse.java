package ar.edu.tpi.contratistas.web.dto;

import java.time.LocalDateTime;

/** Contrato interno GET /internal/maquinas/{id}/disponibilidad. */
public record DisponibilidadResponse(Long maquinaId, LocalDateTime desde, LocalDateTime hasta, boolean disponible,
                                     String motivo) {
}
