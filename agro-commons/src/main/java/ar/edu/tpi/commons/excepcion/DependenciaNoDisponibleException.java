package ar.edu.tpi.commons.excepcion;

import org.springframework.http.HttpStatus;

import java.util.List;

/** 503: un servicio interno o una API externa no respondió o respondió algo inesperado (falla segura). */
public class DependenciaNoDisponibleException extends NegocioException {

    public DependenciaNoDisponibleException(String mensaje) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCIA_NO_DISPONIBLE", mensaje, List.of(), null);
    }

    public DependenciaNoDisponibleException(String mensaje, List<String> detalles) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCIA_NO_DISPONIBLE", mensaje, detalles, null);
    }

    public DependenciaNoDisponibleException(String mensaje, Throwable causa) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCIA_NO_DISPONIBLE", mensaje, List.of(), causa);
    }
}
