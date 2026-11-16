package ar.edu.tpi.commons.error;

import ar.edu.tpi.commons.excepcion.NegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Traduce las excepciones a {@link ApiError} con el código HTTP correspondiente.
 * Se registra automáticamente en cada servicio (ver {@code AgroCommonsAutoConfiguration}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ApiError> negocio(NegocioException ex, HttpServletRequest req) {
        if (ex.getStatus().is5xxServerError()) {
            log.warn("{} {} -> {}: {}", req.getMethod(), req.getRequestURI(), ex.getStatus().value(), ex.getMessage(),
                    ex.getCause());
        } else {
            log.info("{} {} -> {}: {}", req.getMethod(), req.getRequestURI(), ex.getStatus().value(), ex.getMessage());
        }
        return responder(ex.getStatus(), ex.getCodigo(), ex.getMessage(), ex.getDetalles(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalles = ex.getBindingResult().getAllErrors().stream()
                .map(e -> e instanceof FieldError fe ? fe.getField() + ": " + fe.getDefaultMessage() : e.getDefaultMessage())
                .sorted()
                .toList();
        return responder(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Datos inválidos", detalles, req);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> validacionParametros(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<String> detalles = ex.getAllErrors().stream().map(e -> String.valueOf(e.getDefaultMessage())).toList();
        return responder(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Datos inválidos", detalles, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> restriccion(ConstraintViolationException ex, HttpServletRequest req) {
        List<String> detalles = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
        return responder(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Datos inválidos", detalles, req);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> pedidoMalFormado(Exception ex, HttpServletRequest req) {
        String mensaje = ex instanceof HttpMessageNotReadableException
                ? "El cuerpo del pedido no es un JSON válido o tiene un valor con formato incorrecto"
                : ex.getMessage();
        return responder(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", mensaje, List.of(), req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return responder(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", "No tiene permiso para esta operación", List.of(), req);
    }

    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ApiError> rutaInexistente(Exception ex, HttpServletRequest req) {
        HttpStatusCode status = ((ErrorResponse) ex).getStatusCode();
        String codigo = status.value() == 404 ? "NO_ENCONTRADO" : "METODO_NO_PERMITIDO";
        return responder(HttpStatus.valueOf(status.value()), codigo, ex.getMessage(), List.of(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex, HttpServletRequest req) {
        log.error("Error no previsto en {} {}", req.getMethod(), req.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Error interno del servidor", List.of(), req);
    }

    protected ResponseEntity<ApiError> responder(HttpStatus status, String codigo, String mensaje, List<String> detalles,
                                                 HttpServletRequest req) {
        return ResponseEntity.status(status).body(ApiError.of(codigo, mensaje, detalles, req.getRequestURI()));
    }
}
