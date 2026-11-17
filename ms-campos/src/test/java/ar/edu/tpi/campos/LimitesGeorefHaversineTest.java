package ar.edu.tpi.campos;

import ar.edu.tpi.campos.cliente.GeorefClient;
import ar.edu.tpi.campos.cliente.GeorefClient.Localidad;
import ar.edu.tpi.campos.dominio.Zona;
import ar.edu.tpi.campos.servicio.ClasificadorZona;
import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.commons.util.GeoUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Tarjeta 02-13: casos límite de la clasificación (RN-05, RN-08) y de las respuestas de Georef. */
class LimitesGeorefHaversineTest {

    @Nested
    class LimitesDeZona {

        @ParameterizedTest(name = "{0} m con exclusión 1000 y amortiguamiento 3000 -> {1}")
        @CsvSource({
                "0, EXCLUSION",
                "999, EXCLUSION",
                "1000, EXCLUSION",       // exactamente en el límite: criterio conservador
                "1001, AMORTIGUAMIENTO",
                "2999, AMORTIGUAMIENTO",
                "3000, AMORTIGUAMIENTO", // exactamente en el límite
                "3001, LIBRE",
                "50000, LIBRE"})
        void bordesDeCadaZona(long metros, Zona esperada) {
            assertThat(ClasificadorZona.zonaPara(metros, 1000, 3000)).isEqualTo(esperada);
        }

        @Test
        void eligeLaLocalidadMasCercanaAunqueVengaUltima() {
            List<Localidad> locs = List.of(
                    new Localidad("Lejana", -31.5, -64.0),
                    new Localidad("Media", -31.05, -64.0),
                    new Localidad("Cercana", -31.001, -64.0));
            var c = new ClasificadorZona().clasificar(-31.0, -64.0, locs, 1000, 3000);
            assertThat(c.localidadCercana()).isEqualTo("Cercana");
            assertThat(c.zona()).isEqualTo(Zona.EXCLUSION);
        }

        @Test
        void exclusionIgualAAmortiguamientoEsInvalido() {
            assertThatThrownBy(() -> new ClasificadorZona().clasificar(-31, -64, List.of(), 1000, 1000))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void exclusionCeroEsInvalido() {
            assertThatThrownBy(() -> new ClasificadorZona().clasificar(-31, -64, List.of(), 0, 1000))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class LimitesDeHaversine {

        @Test
        void cruzaElMeridianoDe180() {
            assertThat(GeoUtils.haversineKm(0, 179.9, 0, -179.9)).isLessThan(23);
        }

        @Test
        void polosOpuestosEsMediaCircunferencia() {
            assertThat(GeoUtils.haversineKm(90, 0, -90, 0)).isBetween(20010.0, 20020.0);
        }

        @Test
        void distanciaCortaEnElCampoEsPrecisaAlMetro() {
            // 0.001° de latitud ≈ 111 m
            assertThat(GeoUtils.haversineMetros(-33.0, -62.0, -33.001, -62.0)).isBetween(110L, 112L);
        }
    }

    @Nested
    class RespuestasDeGeoref {

        private GeorefClient cliente(String respuesta, String recurso) {
            RestClient.Builder b = RestClient.builder().baseUrl("http://georef");
            MockRestServiceServer server = MockRestServiceServer.bindTo(b).build();
            server.expect(requestTo(startsWith("http://georef/" + recurso)))
                    .andRespond(withSuccess(respuesta, MediaType.APPLICATION_JSON));
            return new GeorefClient(b.build());
        }

        @Test
        void puntoEnArgentinaSinDepartamentoDevuelveUbicacionSinDepto() {
            var u = cliente("""
                    {"ubicacion":{"provincia":{"id":"06","nombre":"Buenos Aires"},"departamento":{"id":null,"nombre":null}}}""",
                    "ubicacion").ubicar(-36, -57).orElseThrow();
            assertThat(u.departamentoId()).isNull();
        }

        @Test
        void departamentoSinLocalidadesDevuelveListaVacia() {
            var locs = cliente("""
                    {"cantidad":0,"localidades":[]}""", "localidades").localidades("14", "14001");
            assertThat(locs).isEmpty();
        }

        @Test
        void jsonMalFormadoEs503() {
            GeorefClient c = cliente("<html>error</html>", "ubicacion");
            assertThatThrownBy(() -> c.ubicar(-31, -64)).isInstanceOf(DependenciaNoDisponibleException.class);
        }

        @Test
        void respuestaSinUbicacionEs503() {
            GeorefClient c = cliente("{}", "ubicacion");
            assertThatThrownBy(() -> c.ubicar(-31, -64)).isInstanceOf(DependenciaNoDisponibleException.class);
        }
    }
}
