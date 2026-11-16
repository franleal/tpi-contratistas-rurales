package ar.edu.tpi.trabajos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Receta agronómica firmada (F-11). Una por orden. */
@Entity
@Table(name = "receta")
@Getter
@Setter
@NoArgsConstructor
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false, unique = true)
    private OrdenTrabajo orden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agronomo_id", nullable = false)
    private Agronomo agronomo;

    @Column(nullable = false, length = 150)
    private String producto;

    /** Ia, Ib, II, III o IV. */
    @Column(name = "categoria_toxicologica", nullable = false, length = 3)
    private String categoriaToxicologica;

    @Column(nullable = false, length = 100)
    private String dosis;

    @Column(name = "firmada_en", nullable = false)
    private LocalDateTime firmadaEn = LocalDateTime.now();
}
