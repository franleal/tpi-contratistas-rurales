package ar.edu.tpi.trabajos.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** F-11. Categoría toxicológica: Ia, Ib, II, III o IV. */
public record RecetaRequest(
        @NotBlank @Size(max = 150) String producto,
        @NotBlank String categoriaToxicologica,
        @NotBlank @Size(max = 100) String dosis) {
}
