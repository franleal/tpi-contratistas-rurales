package ar.edu.tpi.contratistas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aplicador (operario) de la plantilla de un contratista. */
@Entity
@Table(name = "aplicador", uniqueConstraints = @UniqueConstraint(columnNames = {"contratista_id", "documento"}))
@Getter
@Setter
@NoArgsConstructor
public class Aplicador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contratista_id", nullable = false)
    private Contratista contratista;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String documento;

    public Aplicador(Contratista contratista, String nombre, String documento) {
        this.contratista = contratista;
        this.nombre = nombre;
        this.documento = documento;
    }

    public boolean perteneceA(String sub) {
        return contratista.getSub().equals(sub);
    }
}
