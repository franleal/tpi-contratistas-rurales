package ar.edu.tpi.gateway.config;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Security;

/**
 * Resolución DNS de los microservicios.
 * <p>
 * El resolver propio de Reactor Netty guarda las direcciones de {@code ms-campos},
 * {@code ms-contratistas} y {@code ms-trabajos}. Si un servicio se reinicia, Docker puede darle
 * otra IP (incluso la que tenía otro servicio) y el Gateway seguiría enviando las peticiones
 * al contenedor equivocado. Con el resolver de la JVM y un TTL corto, la ruta se corrige sola
 * en pocos segundos sin reiniciar el Gateway.
 */
@Configuration
public class ResolucionDnsConfig {

    static {
        Security.setProperty("networkaddress.cache.ttl", "5");
        Security.setProperty("networkaddress.cache.negative.ttl", "1");
    }

    @Bean
    public HttpClientCustomizer resolverDeLaJvm() {
        return cliente -> cliente.resolver(DefaultAddressResolverGroup.INSTANCE);
    }
}
