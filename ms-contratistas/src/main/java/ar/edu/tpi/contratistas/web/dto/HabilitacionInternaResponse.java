package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.TipoMaquina;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrato interno GET /internal/maquinas/{id}/habilitacion (docs/CONTRATOS.md). Siempre 200
 * si máquina y aplicador existen; {@code habilitado=false} lleva el motivo para el 422 de
 * ms-trabajos. contratistaSub permite aplicar la propiedad de la máquina en ms-trabajos.
 */
public record HabilitacionInternaResponse(Long maquinaId, Long aplicadorId, LocalDate fecha, boolean habilitado,
                                          String motivo, TipoMaquina tipoMaquina, BigDecimal rendimientoHaH,
                                          BigDecimal baseLat, BigDecimal baseLon, String contratistaSub) {
}
