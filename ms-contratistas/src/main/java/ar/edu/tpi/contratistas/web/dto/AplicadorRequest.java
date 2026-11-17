package ar.edu.tpi.contratistas.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AplicadorRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Pattern(regexp = "\\d{7,8}", message = "debe tener 7 u 8 dígitos sin puntos") String documento) {
}
