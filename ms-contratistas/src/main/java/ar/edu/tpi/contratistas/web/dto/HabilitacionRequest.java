package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.TipoHabilitacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * F-05. {@code titularId} es el id del aplicador (CARNET_APLICADOR) o de la máquina
 * (INSPECCION_MAQUINA). Se acepta un vencimiento pasado: queda registrado y bloquea por RN-04.
 */
public record HabilitacionRequest(
        @NotNull TipoHabilitacion tipo,
        @NotNull Long titularId,
        @NotBlank @Size(max = 50) String numero,
        @NotNull LocalDate vencimiento) {
}
