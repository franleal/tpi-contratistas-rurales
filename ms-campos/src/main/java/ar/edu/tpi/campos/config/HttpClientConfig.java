package ar.edu.tpi.campos.config;

import ar.edu.tpi.commons.log.CorrelationIdInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP hacia Georef con timeouts de conexión y de lectura: si Georef no responde
 * a tiempo, el lote no se guarda y se responde 503 (RN-05).
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient georefRestClient(RestClient.Builder builder,
                                       @Value("${georef.url}") String georefUrl,
                                       @Value("${http.connect-timeout-ms:2000}") int connectTimeoutMs,
                                       @Value("${http.read-timeout-ms:8000}") int readTimeoutMs,
                                       CorrelationIdInterceptor correlationId) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        return builder
                .baseUrl(georefUrl)
                .requestFactory(factory)
                .requestInterceptor(correlationId)
                .build();
    }
}
