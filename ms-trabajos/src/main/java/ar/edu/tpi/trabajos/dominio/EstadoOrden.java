package ar.edu.tpi.trabajos.dominio;

/** RN-18: la orden avanza solo en este orden. */
public enum EstadoOrden {
    RECETA_PENDIENTE,
    ABIERTA,
    ASIGNADA,
    EJECUTADA,
    COMPLETADA;

    /** El estado inmediatamente siguiente, o null si es el último. */
    public EstadoOrden siguiente() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }
}
