package ar.edu.tpi.trabajos.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** F-09. Velocidades en km/h, temperatura en °C, humedad en %. */
public record UmbralRequest(
        @NotNull @DecimalMin("0") BigDecimal vientoMinKmh,
        @NotNull @DecimalMin("0") @DecimalMax("200") BigDecimal vientoMaxKmh,
        @NotNull @DecimalMin("0") @DecimalMax("200") BigDecimal rafagaMaxKmh,
        @NotNull @DecimalMin("-20") @DecimalMax("60") BigDecimal temperaturaMaxC,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal humedadMinPct,
        @NotNull @Min(0) @Max(48) Integer horasSinLluviaDespues,
        @NotNull @Min(1) @Max(16) Integer horizonteDias) {
}
