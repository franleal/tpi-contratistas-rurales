package ar.edu.tpi.campos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Campo de un productor. Agrupa sus lotes. */
@Entity
@Table(name = "establecimiento")
@Getter
@Setter
@NoArgsConstructor
public class Establecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "productor_id", nullable = false)
    private Productor productor;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String provincia;

    @OneToMany(mappedBy = "establecimiento")
    @OrderBy("id")
    private List<Lote> lotes = new ArrayList<>();

    public Establecimiento(Productor productor, String nombre, String provincia) {
        this.productor = productor;
        this.nombre = nombre;
        this.provincia = provincia;
    }

    /** Regla de propiedad: el establecimiento es del productor con ese sub. */
    public boolean perteneceA(String sub) {
        return productor != null && productor.getSub().equals(sub);
    }
}
