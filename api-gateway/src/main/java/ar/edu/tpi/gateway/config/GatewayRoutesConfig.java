package ar.edu.tpi.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator configurarRutas(
            RouteLocatorBuilder builder,
            @Value("${services.campos.url}") String camposUrl,
            @Value("${services.contratistas.url}") String contratistasUrl,
            @Value("${services.trabajos.url}") String trabajosUrl) {

        return builder.routes()

                .route("campos", r -> r
                        .path("/api/lotes/**",
                                "/api/establecimientos/**",
                                "/api/admin/zonas/**")
                        .uri(camposUrl))

                .route("contratistas", r -> r
                        .path("/api/contratistas/**",
                                "/api/maquinas/**",
                                "/api/aplicadores/**",
                                "/api/habilitaciones/**",
                                "/api/agenda/**",
                                "/api/admin/habilitaciones/**")
                        .uri(contratistasUrl))

                .route("trabajos", r -> r
                        .path("/api/ordenes/**",
                                "/api/agronomos/**",
                                "/api/admin/matriculas/**",
                                "/api/admin/umbrales/**")
                        .uri(trabajosUrl))

                .build();
    }
}
