package ar.edu.tpi.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/** El Swagger UI unificado es público y lista los tres microservicios. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SwaggerUiTest {

    @Autowired
    WebTestClient web;

    @Test
    void configuracionDelSwaggerUiListaLosServicios() {
        web.get().uri("/v3/api-docs/swagger-config").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.urls.length()").isEqualTo(3)
                .jsonPath("$.urls[?(@.name == 'ms-trabajos')].url").isEqualTo("/trabajos/v3/api-docs");
    }
}
