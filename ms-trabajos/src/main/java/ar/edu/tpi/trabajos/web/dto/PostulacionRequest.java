package ar.edu.tpi.trabajos.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** F-13. El inicio es hora local de Argentina, sin zona (por ejemplo 2026-12-10T08:00). */
public record PostulacionRequest(
        @NotNull Long maquinaId,
        @NotNull Long aplicadorId,
        @NotNull LocalDateTime inicio,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal precioHa) {
}
