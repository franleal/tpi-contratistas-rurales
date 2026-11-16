package ar.edu.tpi.contratistas.dominio;

/**
 * Estados de la validación documental. No existe "VENCIDA": la vigencia se calcula
 * contra la fecha del trabajo en cada operación (RN-04).
 */
public enum EstadoHabilitacion {
    PENDIENTE,
    VALIDADA,
    RECHAZADA
}
