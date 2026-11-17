package ar.edu.tpi.commons.seguridad;

import ar.edu.tpi.commons.error.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 401 y 403 de Spring Security con el formato común de error, para que la colección
 * lea igual los rechazos de seguridad y los de negocio.
 */
public final class RespuestasSeguridad {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private RespuestasSeguridad() {
    }

    public static AuthenticationEntryPoint noAutenticado() {
        return (req, res, ex) -> {
            res.setHeader("WWW-Authenticate", "Bearer");
            escribir(req, res, HttpServletResponse.SC_UNAUTHORIZED, "NO_AUTENTICADO", "Falta el token o no es válido");
        };
    }

    public static AccessDeniedHandler accesoDenegado() {
        return (req, res, ex) -> escribir(req, res, HttpServletResponse.SC_FORBIDDEN, "ACCESO_DENEGADO",
                "No tiene permiso para esta operación");
    }

    private static void escribir(HttpServletRequest req, HttpServletResponse res, int status, String codigo,
                                 String mensaje) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        MAPPER.writeValue(res.getOutputStream(), ApiError.of(codigo, mensaje, List.of(), req.getRequestURI()));
    }
}
