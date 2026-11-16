package ar.edu.tpi.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Seguridad del Gateway (pregunta 10 del enunciado y matriz de permisos):
 * <ul>
 *   <li>valida firma y expiración del JWT con el JWKS de Keycloak;</li>
 *   <li>convierte {@code realm_access.roles} en autoridades {@code ROLE_*};</li>
 *   <li>autoriza por ruta y método HTTP. Sin token o con token inválido: 401. Rol incorrecto: 403.</li>
 * </ul>
 * La propiedad de cada recurso (sub) la controla el servicio, no el Gateway.
 */
@Configuration
public class SecurityConfig {

    public static final String PRODUCTOR = "PRODUCTOR";
    public static final String CONTRATISTA = "CONTRATISTA";
    public static final String AGRONOMO = "AGRONOMO";
    public static final String ADMIN = "ADMIN";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(ex -> ex
                        // Públicos: salud y documentación OpenAPI
                        .pathMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/webjars/**", "/v3/api-docs/**",
                                "/*/v3/api-docs", "/*/v3/api-docs/**").permitAll()

                        // ADMIN: configuración y validación documental
                        .pathMatchers("/api/admin/**").hasRole(ADMIN)

                        // ms-campos
                        .pathMatchers("/api/establecimientos/**", "/api/establecimientos").hasRole(PRODUCTOR)
                        .pathMatchers("/api/lotes/**", "/api/lotes").hasRole(PRODUCTOR)

                        // ms-contratistas
                        .pathMatchers("/api/contratistas/**", "/api/contratistas",
                                "/api/maquinas/**", "/api/maquinas",
                                "/api/aplicadores/**", "/api/aplicadores",
                                "/api/habilitaciones/**", "/api/habilitaciones",
                                "/api/agenda/**").hasRole(CONTRATISTA)

                        // ms-trabajos
                        .pathMatchers("/api/agronomos/**", "/api/agronomos").hasRole(AGRONOMO)
                        .pathMatchers(HttpMethod.POST, "/api/ordenes").hasRole(PRODUCTOR)
                        .pathMatchers(HttpMethod.POST, "/api/ordenes/*/receta").hasRole(AGRONOMO)
                        .pathMatchers(HttpMethod.GET, "/api/ordenes/*/ventanas").hasAnyRole(PRODUCTOR, CONTRATISTA)
                        .pathMatchers(HttpMethod.POST, "/api/ordenes/*/postulaciones").hasRole(CONTRATISTA)
                        .pathMatchers(HttpMethod.GET, "/api/ordenes/*/postulaciones").hasRole(PRODUCTOR)
                        .pathMatchers(HttpMethod.POST, "/api/ordenes/*/asignacion").hasRole(PRODUCTOR)
                        .pathMatchers(HttpMethod.POST, "/api/ordenes/*/ejecucion").hasRole(CONTRATISTA)
                        .pathMatchers(HttpMethod.POST, "/api/ordenes/*/confirmacion").hasRole(PRODUCTOR)
                        .pathMatchers(HttpMethod.GET, "/api/ordenes", "/api/ordenes/*")
                        .hasAnyRole(PRODUCTOR, AGRONOMO, CONTRATISTA)
                        .pathMatchers("/api/ordenes/**", "/api/ordenes").denyAll()

                        // Cualquier otra ruta exige token; si no tiene ruta en el Gateway, 404
                        // (así /internal/** con token válido devuelve 404 y sin token, 401)
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter()))
                        .authenticationEntryPoint((exchange, e) -> escribirError(exchange, HttpStatus.UNAUTHORIZED,
                                "NO_AUTENTICADO", "Falta el token o no es válido")))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((exchange, ex) -> escribirError(exchange, HttpStatus.UNAUTHORIZED,
                                "NO_AUTENTICADO", "Falta el token o no es válido"))
                        .accessDeniedHandler((exchange, ex) -> escribirError(exchange, HttpStatus.FORBIDDEN,
                                "ACCESO_DENEGADO", "El rol del usuario no permite esta operación")))
                .build();
    }

    /** Convierte los roles del realm de Keycloak en autoridades ROLE_*. */
    static Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtConverter() {
        return jwt -> Mono.just(new JwtAuthenticationToken(jwt, rolesDelRealm(jwt), jwt.getSubject()));
    }

    static Collection<GrantedAuthority> rolesDelRealm(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
    }

    /** Mismo formato de error que agro-commons (el Gateway no depende de la librería). */
    private static Mono<Void> escribirError(ServerWebExchange exchange, HttpStatus status, String codigo, String mensaje) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        if (status == HttpStatus.UNAUTHORIZED) {
            response.getHeaders().set("WWW-Authenticate", "Bearer");
        }
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        String json = "{\"codigo\":\"%s\",\"mensaje\":\"%s\",\"detalles\":[],\"timestamp\":\"%s\",\"path\":\"%s\",\"correlationId\":%s}"
                .formatted(codigo, mensaje, Instant.now(), exchange.getRequest().getPath().value().replace("\"", ""),
                        correlationId == null ? "null" : "\"" + correlationId.replace("\"", "") + "\"");
        return response.writeWith(Mono.just(response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8))));
    }
}
