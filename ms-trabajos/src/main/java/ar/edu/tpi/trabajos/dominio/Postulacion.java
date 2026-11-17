package ar.edu.tpi.trabajos.dominio;

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
import java.time.LocalDateTime;

/** Oferta de un contratista sobre una orden ABIERTA (F-13). Solo se guarda si pasó todas las reglas. */
@Entity
@Table(name = "postulacion")
@Getter
@Setter
@NoArgsConstructor
public class Postulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenTrabajo orden;

    @Column(name = "contratista_sub", nullable = false, length = 64)
    private String contratistaSub;

    /** Referencias externas a ms-contratistas. */
    @Column(name = "maquina_id", nullable = false)
    private Long maquinaId;

    @Column(name = "aplicador_id", nullable = false)
    private Long aplicadorId;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(nullable = false)
    private LocalDateTime fin;

    /** inicio - traslado (RN-12): desde aquí se reserva la agenda. */
    @Column(name = "ocupado_desde", nullable = false)
    private LocalDateTime ocupadoDesde;

    @Column(name = "horas_aplicacion", nullable = false, precision = 6, scale = 2)
    private BigDecimal horasAplicacion;

    @Column(name = "traslado_h", nullable = false, precision = 6, scale = 2)
    private BigDecimal trasladoH;

    @Column(name = "precio_ha", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioHa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPostulacion estado = EstadoPostulacion.VALIDA;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();
}
