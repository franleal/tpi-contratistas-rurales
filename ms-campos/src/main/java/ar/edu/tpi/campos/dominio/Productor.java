package ar.edu.tpi.campos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Perfil del productor. Se da de alta automáticamente con el sub del token la primera
 * vez que registra un establecimiento (F-01).
 */
@Entity
@Table(name = "productor")
@Getter
@Setter
@NoArgsConstructor
public class Productor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador del usuario en Keycloak. */
    @Column(nullable = false, unique = true, length = 64)
    private String sub;

    @Column(length = 150)
    private String nombre;

    @Column(length = 150)
    private String email;

    public Productor(String sub, String nombre, String email) {
        this.sub = sub;
        this.nombre = nombre;
        this.email = email;
    }
}
