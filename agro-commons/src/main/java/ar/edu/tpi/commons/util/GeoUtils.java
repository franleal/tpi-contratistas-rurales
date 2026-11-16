package ar.edu.tpi.commons.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cálculos geográficos compartidos: distancia a localidades (ms-campos, RN-05) y
 * traslado de la máquina (ms-trabajos, RN-12).
 */
public final class GeoUtils {

    /** Radio medio de la Tierra en kilómetros. */
    public static final double RADIO_TIERRA_KM = 6371.0088;

    private GeoUtils() {
    }

    /** Distancia de círculo máximo (Haversine) en kilómetros. */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        validar(lat1, lon1);
        validar(lat2, lon2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }

    /** Distancia Haversine en metros, redondeada al metro. */
    public static long haversineMetros(double lat1, double lon1, double lat2, double lon2) {
        return Math.round(haversineKm(lat1, lon1, lat2, lon2) * 1000);
    }

    /** Redondea una coordenada a 6 decimales (≈ 11 cm), la precisión que se guarda. */
    public static double redondearCoordenada(double valor) {
        return BigDecimal.valueOf(valor).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    /** {@code true} si la latitud y la longitud están en rango. */
    public static boolean coordenadaValida(double lat, double lon) {
        return lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    private static void validar(double lat, double lon) {
        if (!coordenadaValida(lat, lon)) {
            throw new IllegalArgumentException("Coordenada fuera de rango: " + lat + ", " + lon);
        }
    }
}
