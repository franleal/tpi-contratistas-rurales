package ar.edu.tpi.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Asigna un {@code X-Correlation-Id} a cada petición que entra por el Gateway (o respeta el
 * que manda el cliente), lo reenvía al servicio y lo devuelve en la respuesta. Corre como
 * WebFilter para que también los 401/403 del Gateway lo lleven, y registra una línea de log
 * de acceso por petición.
 */
@Component
public class CorrelationIdWebFilter implements WebFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern VALIDO = Pattern.compile("[A-Za-z0-9._-]{1,64}");
    private static final Logger log = LoggerFactory.getLogger("acceso");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String id = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (id == null || !VALIDO.matcher(id).matches()) {
            id = UUID.randomUUID().toString();
        }
        String correlationId = id;
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(HEADER, correlationId))
                .build();
        exchange.getResponse().getHeaders().set(HEADER, correlationId);
        long inicio = System.currentTimeMillis();
        return chain.filter(exchange.mutate().request(request).build())
                .doFinally(s -> log.info("[{}] {} {} -> {} ({} ms)", correlationId, request.getMethod(),
                        request.getPath().value(), exchange.getResponse().getStatusCode(),
                        System.currentTimeMillis() - inicio));
    }

    @Override
    public int getOrder() {
        // Antes que la cadena de Spring Security (-100)
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
