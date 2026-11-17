package ar.edu.tpi.contratistas.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContratistaRequest(
        @NotBlank @Size(max = 200) String razonSocial,
        @NotBlank @Pattern(regexp = "\\d{2}-?\\d{8}-?\\d", message = "debe tener 11 dígitos (por ejemplo 30-12345678-9)")
        String cuit) {
}
