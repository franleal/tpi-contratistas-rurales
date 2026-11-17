package ar.edu.tpi.trabajos.web.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** F-16. Los rangos (fin, hectáreas) se validan como regla de negocio RN-15 (422). */
public record EjecucionRequest(@NotNull LocalDateTime inicio, @NotNull LocalDateTime fin,
                               @NotNull BigDecimal hectareas) {
}
