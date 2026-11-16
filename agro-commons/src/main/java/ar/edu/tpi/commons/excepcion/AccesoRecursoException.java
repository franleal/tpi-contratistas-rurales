package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

import java.util.List;

/** 403: el recurso pertenece a otro usuario (regla de propiedad por sub) o falta una habilitación. */
public class AccesoRecursoException extends NegocioException {

    public AccesoRecursoException(String mensaje) {
        super(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", mensaje, List.of(), null);
    }

    public AccesoRecursoException(String mensaje, List<String> detalles) {
        super(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", mensaje, detalles, null);
    }

    public AccesoRecursoException(String mensaje, Throwable causa) {
        super(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", mensaje, List.of(), causa);
    }
}
