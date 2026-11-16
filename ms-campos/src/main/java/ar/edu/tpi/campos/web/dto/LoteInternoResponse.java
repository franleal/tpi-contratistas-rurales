package ar.edu.tpi.campos.web.dto;

import ar.edu.tpi.campos.dominio.Zona;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contrato interno GET /internal/lotes/{id} (docs/CONTRATOS.md). ms-trabajos copia estos
 * datos en la orden y aplica la propiedad comparando productorSub con el sub del token.
 *
 * @param admiteOrdenes {@code false} si el lote está en zona de EXCLUSION: ms-trabajos no
 *                      debe crear la orden y responde 422 (RN-06). La decisión de zona la
 *                      toma ms-campos, dueño de la clasificación.
 */
public record LoteInternoResponse(Long id, String productorSub, String nombre, BigDecimal lat, BigDecimal lon,
                                  BigDecimal superficieHa, String provincia, String departamento, Zona zona,
                                  List<String> categoriasPermitidas, boolean admiteOrdenes) {

    public LoteInternoResponse(Long id, String productorSub, String nombre, BigDecimal lat, BigDecimal lon,
                               BigDecimal superficieHa, String provincia, String departamento, Zona zona,
                               List<String> categoriasPermitidas) {
        this(id, productorSub, nombre, lat, lon, superficieHa, provincia, departamento, zona, categoriasPermitidas,
                zona != Zona.EXCLUSION);
    }
}
