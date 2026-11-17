package ar.edu.tpi.campos.servicio;

import ar.edu.tpi.campos.cliente.GeorefClient.Localidad;
import ar.edu.tpi.campos.dominio.Zona;
import ar.edu.tpi.commons.util.GeoUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Clasificación del lote (RN-05, RN-08): distancia Haversine al centroide de la localidad
 * más cercana del mismo departamento, comparada con las distancias de la provincia.
 * <p>
 * Límites (criterio conservador): a una distancia <b>igual</b> a la de exclusión el lote
 * queda en EXCLUSION, e igual a la de amortiguamiento queda en AMORTIGUAMIENTO.
 * <p>
 * Aproximación declarada: se mide al centroide de la localidad, no al límite del ejido,
 * y solo contra localidades del mismo departamento.
 */
@Component
public class ClasificadorZona {

    /** Resultado de la clasificación. Si no hay localidades, localidad y distancia son nulas. */
    public record Clasificacion(Zona zona, String localidadCercana, Integer distanciaM) {
    }

    public Clasificacion clasificar(double lat, double lon, List<Localidad> localidades,
                                    int distanciaExclusionM, int distanciaAmortiguamientoM) {
        if (distanciaExclusionM <= 0 || distanciaExclusionM >= distanciaAmortiguamientoM) {
            throw new IllegalArgumentException("Se requiere 0 < exclusión < amortiguamiento (RN-08)");
        }
        record Medida(Localidad localidad, long metros) {
        }
        return localidades.stream()
                .map(l -> new Medida(l, GeoUtils.haversineMetros(lat, lon, l.lat(), l.lon())))
                .min(Comparator.comparingLong(Medida::metros))
                .map(m -> new Clasificacion(zonaPara(m.metros(), distanciaExclusionM, distanciaAmortiguamientoM),
                        m.localidad().nombre(), (int) m.metros()))
                .orElse(new Clasificacion(Zona.LIBRE, null, null));
    }

    public static Zona zonaPara(long metros, int exclusionM, int amortiguamientoM) {
        if (metros <= exclusionM) {
            return Zona.EXCLUSION;
        }
        if (metros <= amortiguamientoM) {
            return Zona.AMORTIGUAMIENTO;
        }
        return Zona.LIBRE;
    }
}
