package ar.edu.tpi.trabajos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** F-15: la orden queda asignada a una postulación con su reserva de agenda en ms-contratistas. */
@Entity
@Table(name = "asignacion")
@Getter
@Setter
@NoArgsConstructor
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false, unique = true)
    private OrdenTrabajo orden;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "postulacion_id", nullable = false, unique = true)
    private Postulacion postulacion;

    /** Referencia externa a la reserva de ms-contratistas (para compensar o auditar). */
    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "asignada_en", nullable = false)
    private LocalDateTime asignadaEn = LocalDateTime.now();
}
