package ar.edu.tpi.trabajos.dominio;

import java.util.Arrays;
import java.util.Optional;

/** Clasificación toxicológica de productos fitosanitarios. */
public enum CategoriaToxicologica {
    IA("Ia"), IB("Ib"), II("II"), III("III"), IV("IV");

    private final String codigo;

    CategoriaToxicologica(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<CategoriaToxicologica> desdeCodigo(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(c -> c.codigo.equalsIgnoreCase(valor.trim())).findFirst();
    }
}
