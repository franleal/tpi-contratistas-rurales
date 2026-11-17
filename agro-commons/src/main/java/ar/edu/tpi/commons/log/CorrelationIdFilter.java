package ar.edu.tpi.commons.log;

import ar.edu.tpi.commons.error.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Toma el {@code X-Correlation-Id} que agrega el Gateway (o genera uno si falta), lo deja
 * en el MDC para que aparezca en cada línea de log y lo devuelve en la respuesta. Así una
 * misma operación se puede seguir en los logs del Gateway y de todos los servicios.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        if (id == null || !VALIDO.matcher(id).matches()) {
            id = UUID.randomUUID().toString();
        }
        MDC.put(ApiError.MDC_CORRELATION_ID, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(ApiError.MDC_CORRELATION_ID);
        }
    }
}
