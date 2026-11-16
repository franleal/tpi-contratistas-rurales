package ar.edu.tpi.campos.dominio;

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

/**
 * Lote de un establecimiento, siempre guardado con su clasificación de zona (RN-05):
 * nunca se persiste un lote sin clasificar.
 */
@Entity
@Table(name = "lote")
@Getter
@Setter
@NoArgsConstructor
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establecimiento_id", nullable = false)
    private Establecimiento establecimiento;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lon;

    @Column(name = "superficie_ha", nullable = false, precision = 10, scale = 2)
    private BigDecimal superficieHa;

    @Column(nullable = false, length = 100)
    private String cultivo;

    /** Provincia según Georef (puede diferir de la declarada en el establecimiento). */
    @Column(nullable = false, length = 100)
    private String provincia;

    @Column(nullable = false, length = 150)
    private String departamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Zona zona;

    @Column(name = "localidad_cercana", length = 150)
    private String localidadCercana;

    @Column(name = "distancia_localidad_m")
    private Integer distanciaLocalidadM;

    /** Dueño del lote: el productor de su establecimiento. */
    public String getProductorSub() {
        return establecimiento.getProductor().getSub();
    }
}
