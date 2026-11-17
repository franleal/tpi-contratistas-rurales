package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.TipoMaquina;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * {@code tipo} es opcional: por defecto PULVERIZADORA. El rendimiento admite a lo sumo dos
 * decimales (columna numeric(6,2)): 0.001 se guardaría como 0.00 y dividiría por cero en RN-11.
 */
public record MaquinaRequest(
        TipoMaquina tipo,
        @NotBlank @Size(max = 200) String descripcion,
        @NotNull @Positive @DecimalMax("100") BigDecimal anchoM,
        @NotNull @Positive @DecimalMax("1000") @Digits(integer = 4, fraction = 2) BigDecimal rendimientoHaH,
        @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal baseLat,
        @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal baseLon) {
}
