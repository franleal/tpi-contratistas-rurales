package ar.edu.tpi.trabajos.web.dto;

import jakarta.validation.constraints.NotNull;

public record AsignacionRequest(@NotNull Long postulacionId) {
}
