package ar.edu.tpi.trabajos.web.dto;

import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Zona;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Resumen de la orden con la copia del lote. */
public record OrdenResponse(Long id, String productorSub, Long loteId, String loteNombre, BigDecimal loteLat,
                            BigDecimal loteLon, BigDecimal superficieHa, Zona zona, String provincia,
                            List<String> categoriasPermitidas, LocalDate fechaLimite, EstadoOrden estado,
                            LocalDateTime creadaEn) {

    public static OrdenResponse de(OrdenTrabajo o) {
        return new OrdenResponse(o.getId(), o.getProductorSub(), o.getLoteId(), o.getLoteNombre(), o.getLoteLat(),
                o.getLoteLon(), o.getSuperficieHa(), o.getZona(), o.getProvincia(), o.getCategorias(),
                o.getFechaLimite(), o.getEstado(), o.getCreadaEn());
    }
}
