package ar.edu.tpi.trabajos.web.dto;

/**
 * Detalle de la orden: resumen, receta y, a medida que avanza, asignación y ejecución con
 * su foto climática. Las partes que todavía no existen vienen en null.
 */
public record OrdenDetalleResponse(OrdenResponse orden, RecetaResponse receta, AsignacionResponse asignacion,
                                   EjecucionResponse ejecucion) {

    public OrdenDetalleResponse(OrdenResponse orden, RecetaResponse receta) {
        this(orden, receta, null, null);
    }
}
