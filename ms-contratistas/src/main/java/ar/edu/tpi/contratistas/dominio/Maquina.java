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

import java.math.BigDecimal;

/** Máquina de un contratista. El rendimiento y la base se usan en RN-11 y RN-12. */
@Entity
@Table(name = "maquina")
@Getter
@Setter
@NoArgsConstructor
public class Maquina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contratista_id", nullable = false)
    private Contratista contratista;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMaquina tipo;

    @Column(nullable = false, length = 200)
    private String descripcion;

    /** Ancho de labor en metros. */
    @Column(name = "ancho_m", nullable = false, precision = 5, scale = 2)
    private BigDecimal anchoM;

    /** Hectáreas por hora de aplicación. */
    @Column(name = "rendimiento_ha_h", nullable = false, precision = 6, scale = 2)
    private BigDecimal rendimientoHaH;

    @Column(name = "base_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal baseLat;

    @Column(name = "base_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal baseLon;

    public boolean perteneceA(String sub) {
        return contratista.getSub().equals(sub);
    }
}
