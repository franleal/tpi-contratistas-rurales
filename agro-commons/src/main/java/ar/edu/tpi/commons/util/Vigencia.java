package ar.edu.tpi.commons.util;

import java.time.LocalDate;

/**
 * Vigencia de habilitaciones y matrículas (RN-01, RN-04).
 * <p>
 * La vigencia no se guarda: se calcula contra la fecha del trabajo en cada operación.
 * Un documento que vence un día sigue vigente durante ese día.
 */
public final class Vigencia {

    private Vigencia() {
    }

    /** {@code true} si un documento con ese vencimiento cubre la fecha indicada. */
    public static boolean cubre(LocalDate vencimiento, LocalDate fecha) {
        if (vencimiento == null || fecha == null) {
            return false;
        }
        return !vencimiento.isBefore(fecha);
    }

    /** {@code true} si el documento ya no cubre la fecha (vencido para esa fecha). */
    public static boolean vencidoPara(LocalDate vencimiento, LocalDate fecha) {
        return !cubre(vencimiento, fecha);
    }
}
