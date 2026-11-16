package ar.edu.tpi.commons.error;

import org.slf4j.MDC;

import java.time.Instant;
import java.util.List;

/**
 * Formato único de error de los tres microservicios (ver docs/ERRORES.md).
 *
 * @param codigo        código estable para el cliente (REGLA_NEGOCIO, CONFLICTO, ...)
 * @param mensaje       descripción legible, con la regla que falla cuando corresponde
 * @param detalles      errores por campo u otros datos adicionales
 * @param timestamp     momento del error
 * @param path          ruta pedida
 * @param correlationId identificador de la petición para seguirla en los logs
 */
public record ApiError(String codigo, String mensaje, List<String> detalles, Instant timestamp, String path,
                       String correlationId) {

    /** Clave del MDC donde se guarda el correlation ID de la petición. */
    public static final String MDC_CORRELATION_ID = "correlationId";

    public static ApiError of(String codigo, String mensaje, List<String> detalles, String path) {
        return new ApiError(codigo, mensaje, detalles == null ? List.of() : detalles, Instant.now(), path,
                MDC.get(MDC_CORRELATION_ID));
    }
}
