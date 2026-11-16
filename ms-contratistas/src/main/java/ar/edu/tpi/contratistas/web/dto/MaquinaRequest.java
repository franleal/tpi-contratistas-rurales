package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.TipoMaquina;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** {@code tipo} es opcional: por defecto PULVERIZADORA. */
public record MaquinaRequest(
        TipoMaquina tipo,
        @NotBlank @Size(max = 200) String descripcion,
        @NotNull @Positive @DecimalMax("100") BigDecimal anchoM,
        @NotNull @Positive @DecimalMax("1000") BigDecimal rendimientoHaH,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal baseLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal baseLon) {
}
