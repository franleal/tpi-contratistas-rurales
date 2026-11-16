package ar.edu.tpi.commons.seguridad;

import ar.edu.tpi.commons.excepcion.AccesoRecursoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Datos del usuario autenticado tomados del JWT de Keycloak que está en el contexto
 * de seguridad. Los servicios lo usan para la regla de propiedad por {@code sub}.
 * <p>
 * El control de rol por ruta lo hace el Gateway; {@link #roles()} solo se usa para
 * decisiones que dependen del rol dentro de una misma operación (por ejemplo, qué
 * órdenes listar).
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /** JWT de la petición actual. Lanza 403 si no hay un usuario autenticado con JWT. */
    public static Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new AccesoRecursoException("No hay un usuario autenticado");
    }

    /** Identificador estable del usuario en Keycloak. */
    public static String sub() {
        return jwt().getSubject();
    }

    /** Nombre para mostrar: {@code name}, o {@code preferred_username} si no hay nombre. */
    public static String nombre() {
        Jwt jwt = jwt();
        String nombre = jwt.getClaimAsString("name");
        return nombre != null && !nombre.isBlank() ? nombre : jwt.getClaimAsString("preferred_username");
    }

    public static String email() {
        return jwt().getClaimAsString("email");
    }

    /** Roles del realm ({@code realm_access.roles}). */
    public static List<String> roles() {
        Map<String, Object> realmAccess = jwt().getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream().map(String::valueOf).toList();
    }

    public static boolean tieneRol(String rol) {
        return roles().contains(rol);
    }

    /** Valor crudo del token, para propagarlo en las llamadas /internal. */
    public static String tokenValue() {
        return jwt().getTokenValue();
    }

    /**
     * Regla de propiedad: lanza 403 si el recurso no pertenece al usuario actual.
     *
     * @param subDuenio sub guardado en el recurso
     * @param recurso   descripción para el mensaje ("el lote 7")
     */
    public static void verificarDuenio(String subDuenio, String recurso) {
        if (!Objects.equals(subDuenio, sub())) {
            throw new AccesoRecursoException("No es el dueño de " + recurso);
        }
    }
}
