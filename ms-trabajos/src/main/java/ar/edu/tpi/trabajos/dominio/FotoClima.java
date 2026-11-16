package ar.edu.tpi.trabajos.dominio;

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

/** Clima real de una hora trabajada, con el indicador de si cumplía los umbrales. */
@Entity
@Table(name = "foto_clima")
@Getter
@Setter
@NoArgsConstructor
public class FotoClima {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ejecucion_id", nullable = false)
    private RegistroEjecucion ejecucion;

    @Column(nullable = false)
    private LocalDateTime hora;

    @Column(name = "viento_kmh", nullable = false)
    private double vientoKmh;

    @Column(name = "rafaga_kmh", nullable = false)
    private double rafagaKmh;

    @Column(name = "precipitacion_mm", nullable = false)
    private double precipitacionMm;

    @Column(name = "temperatura_c", nullable = false)
    private double temperaturaC;

    @Column(name = "humedad_pct", nullable = false)
    private double humedadPct;

    @Column(nullable = false)
    private boolean apta;

    @Column(name = "motivo", length = 300)
    private String motivo;
}
