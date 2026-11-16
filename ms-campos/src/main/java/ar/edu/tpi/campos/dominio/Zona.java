package ar.edu.tpi.campos.dominio;

/**
 * Clasificación del lote según su distancia a la localidad más cercana (RN-05, RN-08).
 */
public enum Zona {
    /** Más cerca que la distancia de exclusión: no admite órdenes de pulverización (RN-06). */
    EXCLUSION,
    /** Entre exclusión y amortiguamiento: solo categorías toxicológicas permitidas (RN-07). */
    AMORTIGUAMIENTO,
    /** Más allá de la distancia de amortiguamiento. */
    LIBRE
}
