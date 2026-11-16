package ar.edu.tpi.commons.log;

import ar.edu.tpi.commons.error.ApiError;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Propaga el correlation ID de la petición actual en las llamadas salientes
 * ({@code RestClient}) a otros servicios.
 */
public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String id = MDC.get(ApiError.MDC_CORRELATION_ID);
        if (id != null && !request.getHeaders().containsKey(CorrelationIdFilter.HEADER)) {
            request.getHeaders().set(CorrelationIdFilter.HEADER, id);
        }
        return execution.execute(request, body);
    }
}
