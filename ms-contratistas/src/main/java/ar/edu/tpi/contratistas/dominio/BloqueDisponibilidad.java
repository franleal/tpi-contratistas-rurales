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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Intervalo [desde, hasta) en que una máquina está disponible (F-06). Los bloques de una
 * misma máquina no se superponen entre sí (RN-13). Horas locales de Argentina.
 */
@Entity
@Table(name = "bloque_disponibilidad")
@Getter
@Setter
@NoArgsConstructor
public class BloqueDisponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(nullable = false)
    private LocalDateTime desde;

    @Column(nullable = false)
    private LocalDateTime hasta;

    public BloqueDisponibilidad(Maquina maquina, LocalDateTime desde, LocalDateTime hasta) {
        this.maquina = maquina;
        this.desde = desde;
        this.hasta = hasta;
    }

    /** true si el intervalo [d, h) queda completamente dentro del bloque. */
    public boolean contiene(LocalDateTime d, LocalDateTime h) {
        return !d.isBefore(desde) && !h.isAfter(hasta);
    }
}
