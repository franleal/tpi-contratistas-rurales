package ar.edu.tpi.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Matriz de permisos del Gateway. Los servicios de destino no existen en la prueba: si el
 * Gateway deja pasar la petición, la respuesta es un error de conexión (5xx), nunca 401/403.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "services.campos.url=http://127.0.0.1:9",
        "services.contratistas.url=http://127.0.0.1:9",
        "services.trabajos.url=http://127.0.0.1:9"})
@AutoConfigureWebTestClient
class GatewaySecurityTest {

    @Autowired
    private WebTestClient client;

    @Test
    void sinTokenDevuelve401ConFormatoComun() {
        client.post().uri("/api/lotes").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals("WWW-Authenticate", "Bearer")
                .expectHeader().exists("X-Correlation-Id")
                .expectBody().jsonPath("$.codigo").isEqualTo("NO_AUTENTICADO");
    }

    @Test
    void respetaElCorrelationIdDelCliente() {
        client.get().uri("/actuator/health").header("X-Correlation-Id", "prueba-123").exchange()
                .expectHeader().valueEquals("X-Correlation-Id", "prueba-123");
    }

    @Test
    void healthEsPublico() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }

    @Test
    void contratistaNoPuedeCrearOrden() {
        client.mutateWith(conRol("CONTRATISTA")).post().uri("/api/ordenes").exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.codigo").isEqualTo("ACCESO_DENEGADO");
    }

    @Test
    void internalConTokenValidoDevuelve404() {
        client.mutateWith(conRol("PRODUCTOR")).get().uri("/internal/lotes/1").exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void internalSinTokenDevuelve401() {
        client.get().uri("/internal/lotes/1").exchange().expectStatus().isUnauthorized();
    }

    @ParameterizedTest(name = "{0} {1} con {2} -> permitido={3}")
    @CsvSource({
            "POST, /api/establecimientos, PRODUCTOR, true",
            "POST, /api/establecimientos, CONTRATISTA, false",
            "POST, /api/lotes, PRODUCTOR, true",
            "GET, /api/lotes/1, AGRONOMO, false",
            "PUT, /api/admin/zonas/Córdoba, ADMIN, true",
            "PUT, /api/admin/zonas/Córdoba, PRODUCTOR, false",
            "POST, /api/maquinas, CONTRATISTA, true",
            "POST, /api/maquinas, PRODUCTOR, false",
            "POST, /api/agenda/bloques, CONTRATISTA, true",
            "POST, /api/admin/habilitaciones/1/aprobar, ADMIN, true",
            "POST, /api/admin/habilitaciones/1/aprobar, CONTRATISTA, false",
            "POST, /api/agronomos, AGRONOMO, true",
            "POST, /api/agronomos, PRODUCTOR, false",
            "POST, /api/ordenes, PRODUCTOR, true",
            "POST, /api/ordenes/1/receta, AGRONOMO, true",
            "POST, /api/ordenes/1/receta, PRODUCTOR, false",
            "GET, /api/ordenes/1/ventanas, PRODUCTOR, true",
            "GET, /api/ordenes/1/ventanas, CONTRATISTA, true",
            "GET, /api/ordenes/1/ventanas, AGRONOMO, false",
            "POST, /api/ordenes/1/postulaciones, CONTRATISTA, true",
            "POST, /api/ordenes/1/postulaciones, PRODUCTOR, false",
            "GET, /api/ordenes/1/postulaciones, PRODUCTOR, true",
            "GET, /api/ordenes/1/postulaciones, CONTRATISTA, false",
            "POST, /api/ordenes/1/asignacion, PRODUCTOR, true",
            "POST, /api/ordenes/1/asignacion, CONTRATISTA, false",
            "POST, /api/ordenes/1/ejecucion, CONTRATISTA, true",
            "POST, /api/ordenes/1/ejecucion, PRODUCTOR, false",
            "POST, /api/ordenes/1/confirmacion, PRODUCTOR, true",
            "POST, /api/ordenes/1/confirmacion, CONTRATISTA, false",
            "GET, /api/ordenes, AGRONOMO, true",
            "GET, /api/ordenes/1, CONTRATISTA, true",
            "GET, /api/ordenes, ADMIN, false",
            "PUT, /api/admin/umbrales, ADMIN, true",
            "GET, /api/admin/matriculas, AGRONOMO, false"})
    void matrizDeRoles(String metodo, String path, String rol, boolean permitido) {
        int status = client.mutateWith(conRol(rol)).method(HttpMethod.valueOf(metodo)).uri(path)
                .exchange().returnResult(String.class).getStatus().value();
        if (permitido) {
            assertThat(status).isNotIn(401, 403);
        } else {
            assertThat(status).isEqualTo(403);
        }
    }

    private static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.JwtMutator conRol(String rol) {
        return mockJwt().jwt(j -> j.subject("sub-" + rol.toLowerCase())).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
