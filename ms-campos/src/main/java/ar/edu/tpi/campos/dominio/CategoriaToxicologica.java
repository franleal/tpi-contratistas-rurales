package ar.edu.tpi.campos.dominio;

import java.util.Arrays;
import java.util.Optional;

/** Clasificación toxicológica de productos fitosanitarios (OMS / SENASA). */
public enum CategoriaToxicologica {
    IA("Ia"), IB("Ib"), II("II"), III("III"), IV("IV");

    private final String codigo;

    CategoriaToxicologica(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    /** Busca por código ("Ia", "III"...), sin distinguir mayúsculas. */
    public static Optional<CategoriaToxicologica> desdeCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(c -> c.codigo.equalsIgnoreCase(codigo.trim())).findFirst();
    }
}
