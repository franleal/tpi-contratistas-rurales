package ar.edu.tpi.campos.web.dto;

import ar.edu.tpi.campos.dominio.Zona;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contrato interno GET /internal/lotes/{id} (docs/CONTRATOS.md). ms-trabajos copia estos
 * datos en la orden y aplica la propiedad comparando productorSub con el sub del token.
 */
public record LoteInternoResponse(Long id, String productorSub, String nombre, BigDecimal lat, BigDecimal lon,
                                  BigDecimal superficieHa, String provincia, String departamento, Zona zona,
                                  List<String> categoriasPermitidas) {
}
