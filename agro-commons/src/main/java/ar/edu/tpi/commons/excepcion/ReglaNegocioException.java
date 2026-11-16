package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

import java.util.List;

/** 422: se viola una regla de negocio (RN-xx). El mensaje explica el motivo. */
public class ReglaNegocioException extends NegocioException {

    public ReglaNegocioException(String mensaje) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "REGLA_NEGOCIO", mensaje, List.of(), null);
    }

    public ReglaNegocioException(String mensaje, List<String> detalles) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "REGLA_NEGOCIO", mensaje, detalles, null);
    }

    public ReglaNegocioException(String mensaje, Throwable causa) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "REGLA_NEGOCIO", mensaje, List.of(), causa);
    }
}
