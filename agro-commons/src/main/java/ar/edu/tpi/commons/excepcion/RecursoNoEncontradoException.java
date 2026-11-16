package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

import java.util.List;

/** 404: el recurso pedido no existe. */
public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String mensaje) {
        super(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje, List.of(), null);
    }

    public RecursoNoEncontradoException(String mensaje, List<String> detalles) {
        super(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje, detalles, null);
    }

    public RecursoNoEncontradoException(String mensaje, Throwable causa) {
        super(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", mensaje, List.of(), causa);
    }
}
