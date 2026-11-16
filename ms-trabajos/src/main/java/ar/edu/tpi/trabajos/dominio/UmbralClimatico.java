package ar.edu.tpi.trabajos.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Fila única (id = 1) con los umbrales de RN-09 y el horizonte de RN-10 (F-09). */
@Entity
@Table(name = "umbral_climatico")
@Getter
@Setter
@NoArgsConstructor
public class UmbralClimatico {

    public static final long ID = 1L;
    public static final int HORIZONTE_MAXIMO_DIAS = 16;

    @Id
    private Long id = ID;

    @Column(name = "viento_min_kmh", nullable = false, precision = 5, scale = 1)
    private BigDecimal vientoMinKmh;

    @Column(name = "viento_max_kmh", nullable = false, precision = 5, scale = 1)
    private BigDecimal vientoMaxKmh;

    @Column(name = "rafaga_max_kmh", nullable = false, precision = 5, scale = 1)
    private BigDecimal rafagaMaxKmh;

    @Column(name = "temperatura_max_c", nullable = false, precision = 4, scale = 1)
    private BigDecimal temperaturaMaxC;

    @Column(name = "humedad_min_pct", nullable = false, precision = 4, scale = 1)
    private BigDecimal humedadMinPct;

    @Column(name = "horas_sin_lluvia_despues", nullable = false)
    private Integer horasSinLluviaDespues;

    @Column(name = "horizonte_dias", nullable = false)
    private Integer horizonteDias;
}
