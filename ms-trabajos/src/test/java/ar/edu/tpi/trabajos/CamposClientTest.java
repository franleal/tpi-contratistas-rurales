package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.trabajos.cliente.CamposClient;
import ar.edu.tpi.trabajos.config.PropagacionJwtInterceptor;
import ar.edu.tpi.trabajos.dominio.Zona;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CamposClientTest {

    MockRestServiceServer server;
    CamposClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder b = RestClient.builder().baseUrl("http://campos")
                .requestInterceptor(new PropagacionJwtInterceptor());
        server = MockRestServiceServer.bindTo(b).build();
        cliente = new CamposClient(b.build());
        Jwt jwt = Jwt.withTokenValue("token-p1").header("alg", "none").subject("p1").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtieneElLotePropagandoElToken() {
        server.expect(requestTo("http://campos/internal/lotes/5"))
                .andExpect(header("Authorization", "Bearer token-p1"))
                .andRespond(withSuccess("""
                        {"id":5,"productorSub":"p1","nombre":"Norte","lat":-31.1,"lon":-64.0,"superficieHa":120.5,
                         "provincia":"Córdoba","departamento":"Río Segundo","zona":"AMORTIGUAMIENTO",
                         "categoriasPermitidas":["III","IV"],"admiteOrdenes":true}""", MediaType.APPLICATION_JSON));
        CamposClient.Lote l = cliente.lote(5L);
        assertThat(l.zona()).isEqualTo(Zona.AMORTIGUAMIENTO);
        assertThat(l.categoriasPermitidas()).containsExactly("III", "IV");
        server.verify();
    }

    @Test
    void loteInexistenteSePropagaComo404ConElMensaje() {
        server.expect(requestTo("http://campos/internal/lotes/9"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"codigo\":\"NO_ENCONTRADO\",\"mensaje\":\"No existe el lote 9\"}"));
        assertThatThrownBy(() -> cliente.lote(9L)).isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe el lote 9");
    }

    @Test
    void errorDelServidorEs503() {
        server.expect(requestTo("http://campos/internal/lotes/1")).andRespond(withServerError());
        assertThatThrownBy(() -> cliente.lote(1L)).isInstanceOf(DependenciaNoDisponibleException.class);
    }

    @Test
    void tokenRechazadoPorElInternoEs503() {
        server.expect(requestTo("http://campos/internal/lotes/1")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> cliente.lote(1L)).isInstanceOf(DependenciaNoDisponibleException.class);
    }
}
