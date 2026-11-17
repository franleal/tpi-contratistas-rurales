package ar.edu.tpi.contratistas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Empresa contratista, vinculada al usuario de Keycloak por su sub (F-04). */
@Entity
@Table(name = "contratista")
@Getter
@Setter
@NoArgsConstructor
public class Contratista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sub;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    /** CUIT normalizado: solo los 11 dígitos. */
    @Column(nullable = false, unique = true, length = 11)
    private String cuit;

    public Contratista(String sub, String razonSocial, String cuit) {
        this.sub = sub;
        this.razonSocial = razonSocial;
        this.cuit = cuit;
    }
}
