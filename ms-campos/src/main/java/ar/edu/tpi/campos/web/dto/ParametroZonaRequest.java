package ar.edu.tpi.campos.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ParametroZonaRequest(
        @NotNull @Positive Integer distanciaExclusionM,
        @NotNull @Positive Integer distanciaAmortiguamientoM,
        @NotNull List<String> categoriasPermitidas) {
}
