package ar.edu.tpi.campos;

import ar.edu.tpi.campos.cliente.GeorefClient.Localidad;
import ar.edu.tpi.campos.dominio.Zona;
import ar.edu.tpi.campos.servicio.ClasificadorZona;
import ar.edu.tpi.campos.servicio.ClasificadorZona.Clasificacion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasificadorZonaTest {

    private final ClasificadorZona clasificador = new ClasificadorZona();

    // Un minuto de latitud ≈ 1853 m: permite ubicar puntos a distancias conocidas
    private static final Localidad PUEBLO = new Localidad("Pueblo", -31.0, -64.0);
    private static final Localidad LEJANO = new Localidad("Lejano", -32.0, -64.0);

    @Test
    void cercaDelPuebloQuedaEnExclusion() {
        Clasificacion c = clasificador.clasificar(-31.005, -64.0, List.of(LEJANO, PUEBLO), 1000, 3000);

        assertThat(c.zona()).isEqualTo(Zona.EXCLUSION);
        assertThat(c.localidadCercana()).isEqualTo("Pueblo");
        assertThat(c.distanciaM()).isBetween(550, 560);
    }

    @Test
    void entreDistanciasQuedaEnAmortiguamiento() {
        Clasificacion c = clasificador.clasificar(-31.0 - 1.0 / 60, -64.0, List.of(PUEBLO), 1000, 3000);
        assertThat(c.zona()).isEqualTo(Zona.AMORTIGUAMIENTO);
    }

    @Test
    void lejosQuedaLibre() {
        Clasificacion c = clasificador.clasificar(-31.1, -64.0, List.of(PUEBLO), 1000, 3000);
        assertThat(c.zona()).isEqualTo(Zona.LIBRE);
        assertThat(c.distanciaM()).isGreaterThan(11000);
    }

    @Test
    void sinLocalidadesEnElDepartamentoQuedaLibre() {
        Clasificacion c = clasificador.clasificar(-31.1, -64.0, List.of(), 1000, 3000);
        assertThat(c.zona()).isEqualTo(Zona.LIBRE);
        assertThat(c.localidadCercana()).isNull();
    }

    @Test
    void distanciasInvalidasFallan() {
        assertThatThrownBy(() -> clasificador.clasificar(-31, -64, List.of(PUEBLO), 3000, 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
