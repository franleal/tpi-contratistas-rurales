package ar.edu.tpi.campos.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record LoteRequest(
        @NotNull Long establecimientoId,
        @NotBlank @Size(max = 150) String nombre,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal lon,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal superficieHa,
        @NotBlank @Size(max = 100) String cultivo) {
}
