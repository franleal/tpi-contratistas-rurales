package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

import java.util.List;

/** 409: la operación no corresponde al estado actual (RN-18) o hay un conflicto de agenda. */
public class ConflictoEstadoException extends NegocioException {

    public ConflictoEstadoException(String mensaje) {
        super(HttpStatus.CONFLICT, "CONFLICTO", mensaje, List.of(), null);
    }

    public ConflictoEstadoException(String mensaje, List<String> detalles) {
        super(HttpStatus.CONFLICT, "CONFLICTO", mensaje, detalles, null);
    }

    public ConflictoEstadoException(String mensaje, Throwable causa) {
        super(HttpStatus.CONFLICT, "CONFLICTO", mensaje, List.of(), causa);
    }
}
