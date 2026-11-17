package ar.edu.tpi.campos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

/**
 * Parámetros de zona sensible de una provincia (RN-08): distancias de exclusión y de
 * amortiguamiento (0 &lt; exclusión &lt; amortiguamiento) y categorías toxicológicas permitidas
 * en amortiguamiento. La clave es el nombre de la provincia tal como lo devuelve Georef.
 */
@Entity
@Table(name = "parametro_zona")
@Getter
@Setter
@NoArgsConstructor
public class ParametroZona {

    @Id
    @Column(length = 100)
    private String provincia;

    @Column(name = "distancia_exclusion_m", nullable = false)
    private int distanciaExclusionM;

    @Column(name = "distancia_amortiguamiento_m", nullable = false)
    private int distanciaAmortiguamientoM;

    /** Códigos separados por coma, por ejemplo "III,IV". */
    @Column(name = "categorias_permitidas", nullable = false, length = 50)
    private String categoriasPermitidas;

    public ParametroZona(String provincia, int distanciaExclusionM, int distanciaAmortiguamientoM,
                         List<String> categorias) {
        this.provincia = provincia;
        this.distanciaExclusionM = distanciaExclusionM;
        this.distanciaAmortiguamientoM = distanciaAmortiguamientoM;
        setCategorias(categorias);
    }

    public List<String> getCategorias() {
        if (categoriasPermitidas == null || categoriasPermitidas.isBlank()) {
            return List.of();
        }
        return Arrays.stream(categoriasPermitidas.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public void setCategorias(List<String> categorias) {
        this.categoriasPermitidas = String.join(",", categorias);
    }
}
