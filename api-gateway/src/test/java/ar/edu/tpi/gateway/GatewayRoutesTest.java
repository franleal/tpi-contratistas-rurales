package ar.edu.tpi.gateway;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "services.campos.url=http://campos:8080",
        "services.contratistas.url=http://contratistas:8080",
        "services.trabajos.url=http://trabajos:8080"})
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routeLocator;

    @ParameterizedTest
    @CsvSource({
            "/api/establecimientos, campos",
            "/api/lotes/7, campos",
            "/api/admin/zonas/Córdoba, campos",
            "/api/contratistas, contratistas",
            "/api/maquinas, contratistas",
            "/api/aplicadores, contratistas",
            "/api/habilitaciones, contratistas",
            "/api/agenda/bloques, contratistas",
            "/api/admin/habilitaciones/3/aprobar, contratistas",
            "/api/ordenes, trabajos",
            "/api/ordenes/5/postulaciones, trabajos",
            "/api/agronomos/me, trabajos",
            "/api/admin/matriculas/2/rechazar, trabajos",
            "/api/admin/umbrales, trabajos"})
    void rutaPublicaVaAlServicioDuenio(String path, String servicio) {
        assertThat(rutaPara(path)).isEqualTo(servicio);
    }

    @ParameterizedTest
    @CsvSource({
            "/campos/v3/api-docs, docs-campos",
            "/contratistas/v3/api-docs, docs-contratistas",
            "/trabajos/v3/api-docs, docs-trabajos"})
    void documentacionDeCadaServicio(String path, String ruta) {
        assertThat(rutaPara(path)).isEqualTo(ruta);
    }

    @Test
    void rutasInternasNoSePublican() {
        assertThat(rutaPara("/internal/lotes/1")).isNull();
        assertThat(rutaPara("/internal/maquinas/1/habilitacion")).isNull();
        assertThat(rutaPara("/internal/agenda/reservas")).isNull();
    }

    private String rutaPara(String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
        Flux<Route> rutas = routeLocator.getRoutes();
        return rutas.concatMap(r -> Mono.from(r.getPredicate().apply(exchange)).filter(Boolean::booleanValue).map(ok -> r.getId()))
                .next()
                .block();
    }
}
