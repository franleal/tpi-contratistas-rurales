package ar.edu.tpi.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ruteo del único punto de entrada. Cada prefijo público va al servicio dueño del recurso.
 * <p>
 * Las rutas {@code /internal/**} NO se declaran: solo se usan entre servicios dentro de la
 * red de Docker. Por el Gateway, una petición autenticada a {@code /internal/**} no
 * encuentra ruta y recibe 404 (y sin token, 401 antes de rutear).
 */
@Configuration
public class GatewayRoutesConfig {

    static final String[] RUTAS_CAMPOS = {
            "/api/establecimientos", "/api/establecimientos/**",
            "/api/lotes", "/api/lotes/**",
            "/api/admin/zonas", "/api/admin/zonas/**"};

    static final String[] RUTAS_CONTRATISTAS = {
            "/api/contratistas", "/api/contratistas/**",
            "/api/maquinas", "/api/maquinas/**",
            "/api/aplicadores", "/api/aplicadores/**",
            "/api/habilitaciones", "/api/habilitaciones/**",
            "/api/agenda/**",
            "/api/admin/habilitaciones", "/api/admin/habilitaciones/**"};

    static final String[] RUTAS_TRABAJOS = {
            "/api/ordenes", "/api/ordenes/**",
            "/api/agronomos", "/api/agronomos/**",
            "/api/admin/matriculas", "/api/admin/matriculas/**",
            "/api/admin/umbrales", "/api/admin/umbrales/**"};

    @Bean
    public RouteLocator configurarRutas(
            RouteLocatorBuilder builder,
            @Value("${services.campos.url}") String camposUrl,
            @Value("${services.contratistas.url}") String contratistasUrl,
            @Value("${services.trabajos.url}") String trabajosUrl) {

        return builder.routes()
                .route("campos", r -> r.path(RUTAS_CAMPOS).uri(camposUrl))
                .route("contratistas", r -> r.path(RUTAS_CONTRATISTAS).uri(contratistasUrl))
                .route("trabajos", r -> r.path(RUTAS_TRABAJOS).uri(trabajosUrl))
                .build();
    }
}
