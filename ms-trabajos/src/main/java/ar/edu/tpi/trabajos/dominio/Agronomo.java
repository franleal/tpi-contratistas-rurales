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

import java.time.LocalDate;

/** Agrónomo con su matrícula provincial. Solo firma recetas con matrícula VALIDADA (RN-01). */
@Entity
@Table(name = "agronomo")
@Getter
@Setter
@NoArgsConstructor
public class Agronomo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sub;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String matricula;

    @Column(nullable = false, length = 100)
    private String provincia;

    @Column(nullable = false)
    private LocalDate vencimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMatricula estado = EstadoMatricula.PENDIENTE;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    public void aprobar() {
        exigirPendiente();
        estado = EstadoMatricula.VALIDADA;
        motivoRechazo = null;
    }

    public void rechazar(String motivo) {
        exigirPendiente();
        estado = EstadoMatricula.RECHAZADA;
        motivoRechazo = motivo;
    }

    private void exigirPendiente() {
        if (estado != EstadoMatricula.PENDIENTE) {
            throw new ConflictoEstadoException("La matrícula " + id + " ya fue resuelta (" + estado + ")");
        }
    }
}
