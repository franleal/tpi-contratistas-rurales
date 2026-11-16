package ar.edu.tpi.campos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstablecimientoRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 100) String provincia) {
}
