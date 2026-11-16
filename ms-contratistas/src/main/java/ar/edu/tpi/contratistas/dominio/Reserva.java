package ar.edu.tpi.contratistas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Ocupación de la agenda de una máquina por una orden asignada (F-15). El intervalo incluye
 * el traslado previo. ordenId referencia a ms-trabajos solo por id (sin FK entre esquemas).
 */
@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(nullable = false)
    private LocalDateTime desde;

    @Column(nullable = false)
    private LocalDateTime hasta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado = EstadoReserva.ACTIVA;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    public Reserva(Maquina maquina, Long ordenId, LocalDateTime desde, LocalDateTime hasta) {
        this.maquina = maquina;
        this.ordenId = ordenId;
        this.desde = desde;
        this.hasta = hasta;
    }

    /** Idempotente: liberar una reserva ya liberada no cambia nada. */
    public void liberar() {
        this.estado = EstadoReserva.LIBERADA;
    }
}
