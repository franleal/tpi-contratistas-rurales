package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.util.GeoUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * RN-11 y RN-12. Las duraciones se redondean hacia arriba al minuto, para no subestimar la
 * ocupación de la máquina.
 */
@Component
public class CalculadoraPostulacion {

    private final double factorRuta;
    private final double velocidadKmh;

    public CalculadoraPostulacion(@Value("${traslado.factor-ruta:1.3}") double factorRuta,
                                  @Value("${traslado.velocidad-kmh:40}") double velocidadKmh) {
        if (factorRuta < 1 || velocidadKmh <= 0) {
            throw new IllegalArgumentException("traslado.factor-ruta debe ser >= 1 y traslado.velocidad-kmh > 0");
        }
        this.factorRuta = factorRuta;
        this.velocidadKmh = velocidadKmh;
    }

    /** Resultado del cálculo. Las horas se informan con dos decimales. */
    public record Calculo(BigDecimal horasAplicacion, BigDecimal trasladoH, LocalDateTime inicio,
                          LocalDateTime fin, LocalDateTime ocupadoDesde) {
    }

    public Calculo calcular(BigDecimal superficieHa, BigDecimal rendimientoHaH, double loteLat, double loteLon,
                            double baseLat, double baseLon, LocalDateTime inicio) {
        // RN-11: horas = superficie / rendimiento; fin = inicio + horas
        double horas = superficieHa.doubleValue() / rendimientoHaH.doubleValue();
        // RN-12: traslado = Haversine(base, lote) x factor / velocidad; ocupada desde inicio - traslado
        double km = GeoUtils.haversineKm(baseLat, baseLon, loteLat, loteLon);
        double traslado = km * factorRuta / velocidadKmh;
        return new Calculo(dosDecimales(horas), dosDecimales(traslado), inicio,
                inicio.plusMinutes(minutosHaciaArriba(horas)),
                inicio.minusMinutes(minutosHaciaArriba(traslado)));
    }

    static long minutosHaciaArriba(double horas) {
        // tolera el error de coma flotante (p. ej. 4.000000001 h no suma un minuto)
        return (long) Math.ceil(horas * 60 - 1e-9);
    }

    private static BigDecimal dosDecimales(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
