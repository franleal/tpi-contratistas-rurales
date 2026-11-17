package ar.edu.tpi.trabajos.web.dto;

import ar.edu.tpi.trabajos.dominio.UmbralClimatico;

import java.math.BigDecimal;

public record UmbralResponse(BigDecimal vientoMinKmh, BigDecimal vientoMaxKmh, BigDecimal rafagaMaxKmh,
                             BigDecimal temperaturaMaxC, BigDecimal humedadMinPct, Integer horasSinLluviaDespues,
                             Integer horizonteDias) {

    public static UmbralResponse de(UmbralClimatico u) {
        return new UmbralResponse(u.getVientoMinKmh(), u.getVientoMaxKmh(), u.getRafagaMaxKmh(),
                u.getTemperaturaMaxC(), u.getHumedadMinPct(), u.getHorasSinLluviaDespues(), u.getHorizonteDias());
    }
}
