package ar.edu.tpi.contratistas.dominio;

/** Una reserva ACTIVA ocupa la agenda; al liberarla (compensación) pasa a LIBERADA y deja de ocupar. */
public enum EstadoReserva {
    ACTIVA,
    LIBERADA
}
