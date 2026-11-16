package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.commons.util.Vigencia;
import ar.edu.tpi.contratistas.dominio.Aplicador;
import ar.edu.tpi.contratistas.dominio.EstadoHabilitacion;
import ar.edu.tpi.contratistas.dominio.Habilitacion;
import ar.edu.tpi.contratistas.dominio.Maquina;
import ar.edu.tpi.contratistas.dominio.TipoMaquina;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * RN-02, RN-03 y RN-04: decide si una máquina y un aplicador pueden trabajar en una fecha.
 * La vigencia se calcula contra la fecha del trabajo, no contra hoy.
 */
@Component
public class EvaluadorHabilitacion {

    private final ValidadorHabilitaciones validador;

    public EvaluadorHabilitacion(ValidadorHabilitaciones validador) {
        this.validador = validador;
    }

    /** Resultado de la evaluación: si no está habilitado, el motivo explica qué falla. */
    public record Resultado(boolean habilitado, String motivo) {
        static Resultado ok() {
            return new Resultado(true, null);
        }

        static Resultado no(String motivo) {
            return new Resultado(false, motivo);
        }
    }

    public Resultado evaluar(Maquina maquina, List<Habilitacion> inspecciones,
                             Aplicador aplicador, List<Habilitacion> carnets, LocalDate fecha) {
        if (maquina.getTipo() != TipoMaquina.PULVERIZADORA) {
            return Resultado.no("La máquina " + maquina.getId() + " es de tipo " + maquina.getTipo()
                    + ": solo una PULVERIZADORA puede aplicar (RN-03)");
        }
        if (!aplicador.getContratista().getId().equals(maquina.getContratista().getId())) {
            return Resultado.no("El aplicador " + aplicador.getId() + " no pertenece a la empresa dueña de la máquina");
        }
        Optional<String> falla = fallaDocumento("La inspección técnica de la máquina " + maquina.getId(),
                inspecciones, fecha, "RN-03");
        if (falla.isPresent()) {
            return Resultado.no(falla.get());
        }
        return fallaDocumento("El carnet del aplicador " + aplicador.getId(), carnets, fecha, "RN-02")
                .map(Resultado::no)
                .orElse(Resultado.ok());
    }

    /**
     * Vacío si algún documento está validado y cubre la fecha. Si no, el motivo más útil:
     * vencido para la fecha, pendiente, rechazado o inexistente (RN-04).
     */
    Optional<String> fallaDocumento(String documento, List<Habilitacion> docs, LocalDate fecha, String regla) {
        if (docs.stream().anyMatch(h -> validador.estaValidada(h) && Vigencia.cubre(h.getVencimiento(), fecha))) {
            return Optional.empty();
        }
        Optional<Habilitacion> validadaVencida = docs.stream().filter(validador::estaValidada)
                .max((a, b) -> a.getVencimiento().compareTo(b.getVencimiento()));
        if (validadaVencida.isPresent()) {
            return Optional.of(documento + " vence el " + validadaVencida.get().getVencimiento()
                    + ", antes de la fecha del trabajo " + fecha + " (" + regla + ", RN-04)");
        }
        if (docs.stream().anyMatch(h -> h.getEstado() == EstadoHabilitacion.PENDIENTE)) {
            return Optional.of(documento + " está PENDIENTE de validación (" + regla + ", RN-04)");
        }
        if (docs.stream().anyMatch(h -> h.getEstado() == EstadoHabilitacion.RECHAZADA)) {
            return Optional.of(documento + " fue RECHAZADA (" + regla + ", RN-04)");
        }
        return Optional.of(documento + " no fue cargada (" + regla + ")");
    }
}
