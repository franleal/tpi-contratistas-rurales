package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.contratistas.dominio.Habilitacion;

/**
 * Decide si un documento habilitante está validado. Hoy la validación es documental (la hace
 * el ADMIN) porque no existe una API pública de registros oficiales; la interfaz permite
 * sumar otra implementación (por ejemplo, un registro provincial) sin tocar las reglas.
 */
public interface ValidadorHabilitaciones {

    boolean estaValidada(Habilitacion habilitacion);
}
