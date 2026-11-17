package ar.edu.tpi.trabajos;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Tokens de prueba. El rol lo controla el Gateway, pero ms-trabajos lo usa para filtrar listados. */
final class Usuarios {

    static final RequestPostProcessor P1 = usuario("p1", "PRODUCTOR");
    static final RequestPostProcessor P2 = usuario("p2", "PRODUCTOR");
    static final RequestPostProcessor A1 = usuario("a1", "AGRONOMO");
    static final RequestPostProcessor A2 = usuario("a2", "AGRONOMO");
    static final RequestPostProcessor C1 = usuario("c1", "CONTRATISTA");
    static final RequestPostProcessor C2 = usuario("c2", "CONTRATISTA");
    static final RequestPostProcessor ADMIN = usuario("admin1", "ADMIN");

    private Usuarios() {
    }

    static RequestPostProcessor usuario(String sub, String rol) {
        return jwt().jwt(j -> j.subject(sub).claim("preferred_username", sub).claim("name", "Usuario " + sub)
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of(rol))))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + rol));
    }
}
