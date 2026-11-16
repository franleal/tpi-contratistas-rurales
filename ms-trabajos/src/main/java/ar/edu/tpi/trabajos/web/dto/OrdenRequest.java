package ar.edu.tpi.trabajos.web.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** F-10. La fecha límite es el último día en que puede ejecutarse la aplicación. */
public record OrdenRequest(@NotNull Long loteId, @NotNull @FutureOrPresent LocalDate fechaLimite) {
}
