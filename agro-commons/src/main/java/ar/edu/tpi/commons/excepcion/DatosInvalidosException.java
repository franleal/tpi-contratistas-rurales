package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

import java.util.List;

/** 400: datos de entrada inválidos que Bean Validation no puede expresar (validaciones cruzadas). */
public class DatosInvalidosException extends NegocioException {

    public DatosInvalidosException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", mensaje, List.of(), null);
    }

    public DatosInvalidosException(String mensaje, List<String> detalles) {
        super(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", mensaje, detalles, null);
    }

    public DatosInvalidosException(String mensaje, Throwable causa) {
        super(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", mensaje, List.of(), causa);
    }
}
