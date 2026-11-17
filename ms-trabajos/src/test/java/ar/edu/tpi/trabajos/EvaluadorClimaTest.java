package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient.HoraClima;
import ar.edu.tpi.trabajos.servicio.EvaluadorClima;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RN-09 con los umbrales de referencia. */
class EvaluadorClimaTest {

    static final LocalDateTime D0 = LocalDateTime.of(2026, 12, 10, 0, 0);
    static final LocalDateTime INICIO = LocalDateTime.of(2026, 12, 10, 8, 30);
    static final LocalDateTime FIN = LocalDateTime.of(2026, 12, 10, 11, 15);

    final EvaluadorClima evaluador = new EvaluadorClima();

    Map<LocalDateTime, HoraClima> dia() {
        return Climas.aptas(D0, D0.plusDays(1));
    }

    @Test
    void todasLasHorasAptasEsApta() {
        assertThat(evaluador.evaluar(dia(), INICIO, FIN, Climas.umbral()).apta()).isTrue();
    }

    @Test
    void horasDeAplicacionSonLasQueSeSolapan() {
        assertThat(EvaluadorClima.horasDeAplicacion(INICIO, FIN))
                .containsExactly(D0.withHour(8), D0.withHour(9), D0.withHour(10), D0.withHour(11));
        assertThat(EvaluadorClima.horasDeAplicacion(D0.withHour(8), D0.withHour(10)))
                .containsExactly(D0.withHour(8), D0.withHour(9));
    }

    @ParameterizedTest(name = "viento {0}, ráfaga {1}, temp {2}, humedad {3} -> apta {4}")
    @CsvSource({
            "5, 15, 22, 65, true",     // viento en el mínimo
            "15, 20, 30, 50, true",    // todos en el límite
            "4.9, 15, 22, 65, false",
            "15.1, 15, 22, 65, false",
            "10, 20.1, 22, 65, false",
            "10, 15, 30.1, 65, false",
            "10, 15, 22, 49.9, false"})
    void limitesDeCadaUmbral(double viento, double rafaga, double temp, double humedad, boolean apta) {
        var m = dia();
        LocalDateTime h = D0.withHour(10);
        m.put(h, new HoraClima(h, viento, rafaga, 0, temp, humedad));
        var r = evaluador.evaluar(m, INICIO, FIN, Climas.umbral());
        assertThat(r.apta()).isEqualTo(apta);
        if (!apta) {
            assertThat(r.primeraHoraQueFalla()).isEqualTo(h);
        }
    }

    @Test
    void informaLaPrimeraHoraQueFalla() {
        var m = dia();
        m.put(D0.withHour(9), new HoraClima(D0.withHour(9), 25, 30, 0, 22, 65));
        m.put(D0.withHour(10), new HoraClima(D0.withHour(10), 25, 30, 0, 22, 65));
        var r = evaluador.evaluar(m, INICIO, FIN, Climas.umbral());
        assertThat(r.primeraHoraQueFalla()).isEqualTo(D0.withHour(9));
        assertThat(r.motivo()).contains("viento").contains("RN-09");
    }

    @Test
    void lluviaDentroDeLasNHorasPosterioresNoEsApta() {
        var m = dia();
        LocalDateTime h = D0.withHour(14); // fin 11:15 + 3 h = 14:15 -> la hora 14 cuenta
        m.put(h, new HoraClima(h, 10, 15, 0.4, 22, 65));
        var r = evaluador.evaluar(m, INICIO, FIN, Climas.umbral());
        assertThat(r.apta()).isFalse();
        assertThat(r.motivo()).contains("lluvia");
    }

    @Test
    void lluviaDespuesDeLasNHorasNoImporta() {
        var m = dia();
        LocalDateTime h = D0.withHour(15);
        m.put(h, new HoraClima(h, 10, 15, 5, 22, 65));
        assertThat(evaluador.evaluar(m, INICIO, FIN, Climas.umbral()).apta()).isTrue();
    }

    @Test
    void horaFaltanteEnElPronosticoEs503() {
        var m = dia();
        m.remove(D0.withHour(9));
        assertThatThrownBy(() -> evaluador.evaluar(m, INICIO, FIN, Climas.umbral()))
                .isInstanceOf(DependenciaNoDisponibleException.class);
    }
}
