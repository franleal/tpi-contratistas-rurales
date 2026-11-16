package ar.edu.tpi.commons;

import ar.edu.tpi.commons.excepcion.AccesoRecursoException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.commons.util.GeoUtils;
import ar.edu.tpi.commons.util.Vigencia;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class UtilidadesTest {

    @Nested
    class VigenciaTest {
        private final LocalDate trabajo = LocalDate.of(2026, 11, 20);

        @Test
        void venceDespuesOElMismoDiaCubre() {
            assertThat(Vigencia.cubre(trabajo.plusDays(1), trabajo)).isTrue();
            assertThat(Vigencia.cubre(trabajo, trabajo)).isTrue();
        }

        @Test
        void venceAntesDelTrabajoNoCubreAunqueHoyEsteVigente() {
            // RN-04: se evalúa en la fecha del trabajo, no hoy
            assertThat(Vigencia.cubre(trabajo.minusDays(1), trabajo)).isFalse();
            assertThat(Vigencia.vencidoPara(trabajo.minusDays(1), trabajo)).isTrue();
        }

        @Test
        void sinVencimientoNoCubre() {
            assertThat(Vigencia.cubre(null, trabajo)).isFalse();
        }
    }

    @Nested
    class GeoUtilsTest {
        @Test
        void mismoPuntoEsCero() {
            assertThat(GeoUtils.haversineKm(-31.4, -64.18, -31.4, -64.18)).isZero();
        }

        @Test
        void cordobaRosario() {
            // ~ 378 km en línea recta
            assertThat(GeoUtils.haversineKm(-31.4201, -64.1888, -32.9468, -60.6393)).isCloseTo(378, within(5.0));
        }

        @Test
        void unMinutoDeLatitudEsUnaMillaNautica() {
            assertThat(GeoUtils.haversineMetros(-31.0, -64.0, -31.0 - 1.0 / 60, -64.0)).isCloseTo(1853L, within(3L));
        }

        @Test
        void esSimetrica() {
            assertThat(GeoUtils.haversineKm(-31, -64, -33, -61))
                    .isCloseTo(GeoUtils.haversineKm(-33, -61, -31, -64), within(1e-9));
        }

        @Test
        void redondeaASeisDecimales() {
            assertThat(GeoUtils.redondearCoordenada(-31.12345678)).isEqualTo(-31.123457);
        }

        @Test
        void fueraDeRangoFalla() {
            assertThatThrownBy(() -> GeoUtils.haversineKm(95, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class CurrentUserTest {
        @AfterEach
        void limpiar() {
            SecurityContextHolder.clearContext();
        }

        private void autenticar() {
            Jwt jwt = Jwt.withTokenValue("token-crudo").header("alg", "none")
                    .subject("sub-productor1")
                    .claim("preferred_username", "productor1")
                    .claim("realm_access", Map.of("roles", List.of("PRODUCTOR", "default-roles-agro")))
                    .build();
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        }

        @Test
        void leeSubNombreYRoles() {
            autenticar();
            assertThat(CurrentUser.sub()).isEqualTo("sub-productor1");
            assertThat(CurrentUser.nombre()).isEqualTo("productor1");
            assertThat(CurrentUser.roles()).contains("PRODUCTOR");
            assertThat(CurrentUser.tieneRol("ADMIN")).isFalse();
            assertThat(CurrentUser.tokenValue()).isEqualTo("token-crudo");
        }

        @Test
        void propiedadAjenaDevuelve403() {
            autenticar();
            CurrentUser.verificarDuenio("sub-productor1", "el lote 1");
            assertThatThrownBy(() -> CurrentUser.verificarDuenio("otro", "el lote 2"))
                    .isInstanceOf(AccesoRecursoException.class);
        }

        @Test
        void sinAutenticacionDevuelve403() {
            assertThatThrownBy(CurrentUser::sub).isInstanceOf(AccesoRecursoException.class);
        }
    }
}
