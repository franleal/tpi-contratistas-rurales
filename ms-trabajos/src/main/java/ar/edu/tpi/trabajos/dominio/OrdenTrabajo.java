package ar.edu.tpi.trabajos.dominio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Orden de pulverización sobre un lote (F-10). Guarda una copia mínima del lote para no
 * depender de ms-campos al leerla ni al firmar la receta (D-05).
 */
@Entity
@Table(name = "orden_trabajo")
@Getter
@Setter
@NoArgsConstructor
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "productor_sub", nullable = false, length = 64)
    private String productorSub;

    /** Referencia externa a ms-campos (sin FK entre esquemas). */
    @Column(name = "lote_id", nullable = false)
    private Long loteId;

    @Column(name = "lote_nombre", length = 150)
    private String loteNombre;

    @Column(name = "lote_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal loteLat;

    @Column(name = "lote_lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal loteLon;

    @Column(name = "superficie_ha", nullable = false, precision = 10, scale = 2)
    private BigDecimal superficieHa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Zona zona;

    @Column(nullable = false, length = 100)
    private String provincia;

    /** Copia de las categorías permitidas en amortiguamiento al crear la orden (RN-07), p. ej. "III,IV". */
    @Column(name = "categorias_permitidas", length = 50)
    private String categoriasPermitidas;

    @Column(name = "fecha_limite", nullable = false)
    private LocalDate fechaLimite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado = EstadoOrden.RECETA_PENDIENTE;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn = LocalDateTime.now();

    public List<String> getCategorias() {
        if (categoriasPermitidas == null || categoriasPermitidas.isBlank()) {
            return List.of();
        }
        return Arrays.stream(categoriasPermitidas.split(",")).map(String::trim).toList();
    }

    public void setCategorias(List<String> categorias) {
        this.categoriasPermitidas = categorias == null ? "" : String.join(",", categorias);
    }

    /** 409 si la orden no está en el estado que la operación necesita (RN-18). */
    public void exigirEstado(EstadoOrden esperado, String operacion) {
        if (estado != esperado) {
            throw new ConflictoEstadoException("No se puede " + operacion + ": la orden " + id + " está en "
                    + estado + " y debe estar en " + esperado + " (RN-18)");
        }
    }

    /** Avanza al estado siguiente; cualquier salto o retroceso es 409 (RN-18). */
    public void avanzarA(EstadoOrden destino) {
        if (estado.siguiente() != destino) {
            throw new ConflictoEstadoException("Transición inválida de la orden " + id + ": " + estado + " → "
                    + destino + " (RN-18)");
        }
        estado = destino;
    }

    public boolean perteneceA(String sub) {
        return productorSub.equals(sub);
    }
}
