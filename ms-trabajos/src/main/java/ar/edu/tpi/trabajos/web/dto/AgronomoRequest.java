package ar.edu.tpi.trabajos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** F-07. El nombre se toma del token. */
public record AgronomoRequest(
        @NotBlank @Size(max = 50) String matricula,
        @NotBlank @Size(max = 100) String provincia,
        @NotNull LocalDate vencimiento) {
}
