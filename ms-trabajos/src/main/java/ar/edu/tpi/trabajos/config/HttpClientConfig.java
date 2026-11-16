package ar.edu.tpi.trabajos.config;

import ar.edu.tpi.commons.log.CorrelationIdInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Clientes HTTP con timeouts. Los internos propagan el JWT y el correlation ID; Open-Meteo
 * solo el correlation ID. Si una dependencia no responde a tiempo se responde 503.
 */
@Configuration
public class HttpClientConfig {

    private final SimpleClientHttpRequestFactory factory;

    public HttpClientConfig(@Value("${http.connect-timeout-ms:2000}") int connectTimeoutMs,
                            @Value("${http.read-timeout-ms:8000}") int readTimeoutMs) {
        factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
    }

    @Bean
    public RestClient camposRestClient(RestClient.Builder builder, @Value("${services.campos.url}") String url,
                                       CorrelationIdInterceptor correlationId, PropagacionJwtInterceptor jwt) {
        return builder.clone().baseUrl(url).requestFactory(factory)
                .requestInterceptor(correlationId).requestInterceptor(jwt).build();
    }

    @Bean
    public RestClient contratistasRestClient(RestClient.Builder builder,
                                             @Value("${services.contratistas.url}") String url,
                                             CorrelationIdInterceptor correlationId, PropagacionJwtInterceptor jwt) {
        return builder.clone().baseUrl(url).requestFactory(factory)
                .requestInterceptor(correlationId).requestInterceptor(jwt).build();
    }

    @Bean
    public RestClient openMeteoRestClient(RestClient.Builder builder, @Value("${open-meteo.url}") String url,
                                          CorrelationIdInterceptor correlationId) {
        return builder.clone().baseUrl(url).requestFactory(factory).requestInterceptor(correlationId).build();
    }
}
