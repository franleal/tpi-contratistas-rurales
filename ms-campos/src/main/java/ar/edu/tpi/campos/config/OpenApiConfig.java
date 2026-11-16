package ar.edu.tpi.campos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI. El servidor es relativo ("/") para que, vista desde el Swagger UI del
 * Gateway, las pruebas pasen por el Gateway con el token de Keycloak.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("ms-campos").version("1.0.0")
                        .description("Productores, establecimientos, lotes con clasificación de zona (Georef) y parámetros de zona por provincia. Errores con el formato común ApiError (docs/ERRORES.md)."))
                .addServersItem(new Server().url("/"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /** Las rutas /internal no se publican en el Gateway: tampoco se documentan. */
    @Bean
    public OpenApiCustomizer ocultarInternas() {
        return api -> {
            if (api.getPaths() != null) {
                api.getPaths().keySet().removeIf(ruta -> ruta.startsWith("/internal"));
            }
        };
    }
}
