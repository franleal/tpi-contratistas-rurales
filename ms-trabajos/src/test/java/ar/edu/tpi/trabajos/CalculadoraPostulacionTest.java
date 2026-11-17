package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.servicio.CalculadoraPostulacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RN-11 y RN-12 con factor 1.3 y 40 km/h. */
class CalculadoraPostulacionTest {

    static final LocalDateTime INICIO = LocalDateTime.of(2026, 12, 10, 8, 0);

    final CalculadoraPostulacion calc = new CalculadoraPostulacion(1.3, 40);

    /** RN-11: un rendimiento 0.00 (p. ej. guardado antes de validar la escala) no divide por cero. */
    @Test
    void rendimientoCeroSeRechazaSinDividir() {
        assertThatThrownBy(() -> calc.calcular(new BigDecimal("100"), new BigDecimal("0.00"), 0, 0, 0, 0, INICIO))
                .isInstanceOf(ar.edu.tpi.commons.excepcion.ReglaNegocioException.class)
                .hasMessageContaining("rendimiento");
    }

    @Test
    void horasDeAplicacionYFin() {
        // 100 ha / 25 ha/h = 4 h exactas
        var c = calc.calcular(new BigDecimal("100"), new BigDecimal("25"), -31.4, -64.2, -31.4, -64.2, INICIO);
        assertThat(c.horasAplicacion()).isEqualByComparingTo("4.00");
        assertThat(c.fin()).isEqualTo(INICIO.plusHours(4));
        assertThat(c.trasladoH()).isEqualByComparingTo("0.00");
        assertThat(c.ocupadoDesde()).isEqualTo(INICIO);
    }

    @Test
    void elFinSeRedondeaHaciaArribaAlMinuto() {
        // 100 / 30 = 3.333 h = 200 min exactos; 101 / 30 = 3.3667 h = 202.0 min -> 202
        var c = calc.calcular(new BigDecimal("101"), new BigDecimal("30"), 0, 0, 0, 0, INICIO);
        assertThat(c.fin()).isEqualTo(INICIO.plusMinutes(202));
        var c2 = calc.calcular(new BigDecimal("100.5"), new BigDecimal("30"), 0, 0, 0, 0, INICIO);
        assertThat(c2.fin()).isEqualTo(INICIO.plusMinutes(201)); // 201 min exactos
    }

    @Test
    void trasladoDesdeLaBase() {
        // 1° de latitud ≈ 111.2 km; x 1.3 / 40 ≈ 3.61 h -> ocupada desde ≈ 3 h 37 min antes
        var c = calc.calcular(new BigDecimal("50"), new BigDecimal("25"), -31.0, -64.0, -32.0, -64.0, INICIO);
        assertThat(c.trasladoH()).isEqualByComparingTo("3.61");
        assertThat(c.ocupadoDesde()).isEqualTo(INICIO.minusMinutes(217));
    }

    @Test
    void configuracionInvalida() {
        assertThatThrownBy(() -> new CalculadoraPostulacion(0.5, 40)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CalculadoraPostulacion(1.3, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
