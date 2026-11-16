package ar.edu.tpi.trabajos.cliente;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.commons.excepcion.NegocioException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Traduce las respuestas de los servicios internos al formato común: 404, 409 y 422 se
 * propagan con el mensaje del servicio llamado; cualquier otra falla (timeout, 5xx, 401,
 * respuesta inesperada) es 503 DEPENDENCIA_NO_DISPONIBLE.
 */
final class ErroresInternos {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ErroresInternos() {
    }

    static <T> T llamar(String servicio, Supplier<T> llamada) {
        T r;
        try {
            r = llamada.get();
        } catch (RestClientResponseException e) {
            throw traducir(servicio, e);
        } catch (RestClientException e) {
            throw new DependenciaNoDisponibleException(servicio + " no está disponible", e);
        }
        if (r == null) {
            throw new DependenciaNoDisponibleException(servicio + " devolvió una respuesta vacía");
        }
        return r;
    }

    static void ejecutar(String servicio, Runnable llamada) {
        llamar(servicio, () -> {
            llamada.run();
            return Boolean.TRUE;
        });
    }

    private static NegocioException traducir(String servicio, RestClientResponseException e) {
        String mensaje = mensaje(e);
        return switch (e.getStatusCode().value()) {
            case 404 -> new RecursoNoEncontradoException(mensaje);
            case 409 -> new ConflictoEstadoException(mensaje);
            case 422 -> new ReglaNegocioException(mensaje);
            default -> new DependenciaNoDisponibleException(servicio + " respondió " + e.getStatusCode().value(), e);
        };
    }

    private static String mensaje(RestClientResponseException e) {
        try {
            JsonNode n = JSON.readTree(e.getResponseBodyAsString());
            if (n != null && n.hasNonNull("mensaje")) {
                return n.get("mensaje").asText();
            }
        } catch (Exception ignorada) {
            // cuerpo no JSON: se usa el estado
        }
        return "Error " + e.getStatusCode().value() + " del servicio interno";
    }
}
