package ar.edu.tpi.contratistas;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Tokens de prueba (el rol lo controla el Gateway; acá solo importa el sub). */
final class Usuarios {

    static final RequestPostProcessor C1 = usuario("c1");
    static final RequestPostProcessor C2 = usuario("c2");
    static final RequestPostProcessor ADMIN = usuario("admin1");

    private Usuarios() {
    }

    static RequestPostProcessor usuario(String sub) {
        return jwt().jwt(j -> j.subject(sub).claim("preferred_username", sub));
    }
}
