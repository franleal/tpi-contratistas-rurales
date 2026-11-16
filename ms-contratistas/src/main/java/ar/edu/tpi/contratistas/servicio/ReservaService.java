package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import ar.edu.tpi.contratistas.dominio.Maquina;
import ar.edu.tpi.contratistas.dominio.Reserva;
import ar.edu.tpi.contratistas.repositorio.BloqueDisponibilidadRepository;
import ar.edu.tpi.contratistas.repositorio.MaquinaRepository;
import ar.edu.tpi.contratistas.repositorio.ReservaRepository;
import ar.edu.tpi.contratistas.web.dto.DisponibilidadResponse;
import ar.edu.tpi.contratistas.web.dto.ReservaRequest;
import ar.edu.tpi.contratistas.web.dto.ReservaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/** RN-13: disponibilidad y reservas de agenda pedidas por ms-trabajos. */
@Service
public class ReservaService {

    private static final Logger log = LoggerFactory.getLogger(ReservaService.class);

    private final MaquinaRepository maquinas;
    private final BloqueDisponibilidadRepository bloques;
    private final ReservaRepository reservas;

    public ReservaService(MaquinaRepository maquinas, BloqueDisponibilidadRepository bloques,
                          ReservaRepository reservas) {
        this.maquinas = maquinas;
        this.bloques = bloques;
        this.reservas = reservas;
    }

    /** F-13: consulta sin efectos. El resultado puede cambiar hasta que se reserve (F-15). */
    @Transactional(readOnly = true)
    public DisponibilidadResponse disponibilidad(Long maquinaId, LocalDateTime desde, LocalDateTime hasta) {
        AgendaService.validarIntervalo(desde, hasta);
        if (!maquinas.existsById(maquinaId)) {
            throw new RecursoNoEncontradoException("No existe la máquina " + maquinaId);
        }
        Optional<String> motivo = motivoNoDisponible(maquinaId, desde, hasta);
        return new DisponibilidadResponse(maquinaId, desde, hasta, motivo.isEmpty(), motivo.orElse(null));
    }

    /**
     * F-15: reserva el intervalo. La fila de la máquina se bloquea (SELECT ... FOR UPDATE, D-08)
     * antes de controlar la superposición, así dos asignaciones simultáneas sobre la misma
     * máquina se serializan y la segunda recibe 409.
     */
    @Transactional
    public ReservaResponse reservar(ReservaRequest req) {
        AgendaService.validarIntervalo(req.desde(), req.hasta());
        Maquina m = maquinas.bloquear(req.maquinaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la máquina " + req.maquinaId()));
        if (bloques.buscarQueContiene(m.getId(), req.desde(), req.hasta()).isEmpty()) {
            throw new ReglaNegocioException("El intervalo " + req.desde() + " a " + req.hasta()
                    + " no está dentro de un bloque de disponibilidad de la máquina " + m.getId() + " (RN-13)");
        }
        if (reservas.existeActivaSuperpuesta(m.getId(), req.desde(), req.hasta())) {
            throw new ConflictoEstadoException("La máquina " + m.getId() + " ya tiene una reserva entre "
                    + req.desde() + " y " + req.hasta() + " (RN-13)");
        }
        Reserva r = reservas.save(new Reserva(m, req.ordenId(), req.desde(), req.hasta()));
        log.info("Reserva {} creada: máquina {} orden {} [{} - {})", r.getId(), m.getId(), req.ordenId(),
                req.desde(), req.hasta());
        return ReservaResponse.de(r);
    }

    private Optional<String> motivoNoDisponible(Long maquinaId, LocalDateTime desde, LocalDateTime hasta) {
        if (bloques.buscarQueContiene(maquinaId, desde, hasta).isEmpty()) {
            return Optional.of("El intervalo " + desde + " a " + hasta
                    + " no está dentro de un bloque de disponibilidad de la máquina (RN-13)");
        }
        if (reservas.existeActivaSuperpuesta(maquinaId, desde, hasta)) {
            return Optional.of("La máquina ya tiene una reserva que se superpone con " + desde + " a " + hasta
                    + " (RN-13)");
        }
        return Optional.empty();
    }
}
