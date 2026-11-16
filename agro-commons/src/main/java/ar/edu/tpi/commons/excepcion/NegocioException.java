package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Base de las excepciones de negocio. Cada subclase fija el código HTTP y el código
 * de error, así los tres servicios traducen sus errores exactamente igual.
 */
public abstract class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final List<String> detalles;

    protected NegocioException(HttpStatus status, String codigo, String mensaje, List<String> detalles, Throwable causa) {
        super(mensaje, causa);
        this.status = status;
        this.codigo = codigo;
        this.detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public List<String> getDetalles() {
        return detalles;
    }
}
