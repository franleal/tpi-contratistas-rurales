package ar.edu.tpi.trabajos.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** F-16: lo que realmente se aplicó, con la foto del clima de cada hora (RN-16). */
@Entity
@Table(name = "registro_ejecucion")
@Getter
@Setter
@NoArgsConstructor
public class RegistroEjecucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false, unique = true)
    private OrdenTrabajo orden;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hectareas;

    /** false si Open-Meteo no respondió: la ejecución se registra igual (RN-16). */
    @Column(name = "clima_disponible", nullable = false)
    private boolean climaDisponible;

    /** true si alguna hora trabajada estuvo fuera de umbral. */
    @Column(name = "fuera_de_condiciones", nullable = false)
    private boolean fueraDeCondiciones;

    @Column(name = "registrada_en", nullable = false)
    private LocalDateTime registradaEn = LocalDateTime.now();

    @Column(name = "confirmada_en")
    private LocalDateTime confirmadaEn;

    @OneToMany(mappedBy = "ejecucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("hora ASC")
    private List<FotoClima> fotos = new ArrayList<>();

    public void agregarFoto(FotoClima f) {
        f.setEjecucion(this);
        fotos.add(f);
    }
}
