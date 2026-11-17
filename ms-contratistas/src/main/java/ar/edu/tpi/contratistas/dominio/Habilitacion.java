package ar.edu.tpi.contratistas.dominio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Carnet de un aplicador o inspección técnica de una máquina (F-05). Nace PENDIENTE y el
 * ADMIN la aprueba o la rechaza una sola vez (F-08). Tiene exactamente un titular.
 */
@Entity
@Table(name = "habilitacion")
@Getter
@Setter
@NoArgsConstructor
public class Habilitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoHabilitacion tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aplicador_id")
    private Aplicador aplicador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_id")
    private Maquina maquina;

    @Column(nullable = false, length = 50)
    private String numero;

    @Column(nullable = false)
    private LocalDate vencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoHabilitacion estado = EstadoHabilitacion.PENDIENTE;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    @Column(name = "cargada_en", nullable = false)
    private LocalDateTime cargadaEn = LocalDateTime.now();

    public static Habilitacion carnet(Aplicador aplicador, String numero, LocalDate vencimiento) {
        Habilitacion h = new Habilitacion();
        h.tipo = TipoHabilitacion.CARNET_APLICADOR;
        h.aplicador = aplicador;
        h.numero = numero;
        h.vencimiento = vencimiento;
        return h;
    }

    public static Habilitacion inspeccion(Maquina maquina, String numero, LocalDate vencimiento) {
        Habilitacion h = new Habilitacion();
        h.tipo = TipoHabilitacion.INSPECCION_MAQUINA;
        h.maquina = maquina;
        h.numero = numero;
        h.vencimiento = vencimiento;
        return h;
    }

    /** F-08: aprobación documental. 409 si ya estaba resuelta. */
    public void aprobar() {
        exigirPendiente();
        this.estado = EstadoHabilitacion.VALIDADA;
        this.motivoRechazo = null;
    }

    /** F-08: rechazo con motivo. 409 si ya estaba resuelta. */
    public void rechazar(String motivo) {
        exigirPendiente();
        this.estado = EstadoHabilitacion.RECHAZADA;
        this.motivoRechazo = motivo;
    }

    private void exigirPendiente() {
        if (estado != EstadoHabilitacion.PENDIENTE) {
            throw new ConflictoEstadoException("La habilitación " + id + " ya fue resuelta (" + estado + ")");
        }
    }

    /** Sub del contratista dueño del titular. */
    public String getContratistaSub() {
        return tipo == TipoHabilitacion.CARNET_APLICADOR
                ? aplicador.getContratista().getSub()
                : maquina.getContratista().getSub();
    }
}
