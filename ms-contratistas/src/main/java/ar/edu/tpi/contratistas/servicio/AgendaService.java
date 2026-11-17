package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.DatosInvalidosException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.contratistas.dominio.BloqueDisponibilidad;
import ar.edu.tpi.contratistas.dominio.Maquina;
import ar.edu.tpi.contratistas.repositorio.BloqueDisponibilidadRepository;
import ar.edu.tpi.contratistas.repositorio.MaquinaRepository;
import ar.edu.tpi.contratistas.repositorio.ReservaRepository;
import ar.edu.tpi.contratistas.web.dto.AgendaResponse;
import ar.edu.tpi.contratistas.web.dto.BloqueRequest;
import ar.edu.tpi.contratistas.web.dto.BloqueResponse;
import ar.edu.tpi.contratistas.web.dto.ReservaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** F-06 y consulta de agenda de una máquina propia. */
@Service
public class AgendaService {

    private final MaquinaRepository maquinas;
    private final BloqueDisponibilidadRepository bloques;
    private final ReservaRepository reservas;

    public AgendaService(MaquinaRepository maquinas, BloqueDisponibilidadRepository bloques, ReservaRepository reservas) {
        this.maquinas = maquinas;
        this.bloques = bloques;
        this.reservas = reservas;
    }

    /**
     * Declara un bloque sin superposición (RN-13). Bloquea la fila de la máquina (D-08) para
     * que dos declaraciones simultáneas no pasen ambas el control.
     */
    @Transactional
    public BloqueResponse declarar(BloqueRequest req) {
        validarIntervalo(req.desde(), req.hasta());
        Maquina m = maquinas.bloquear(req.maquinaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la máquina " + req.maquinaId()));
        CurrentUser.verificarDuenio(m.getContratista().getSub(), "la máquina " + m.getId());
        if (bloques.existeSuperpuesto(m.getId(), req.desde(), req.hasta())) {
            throw new ConflictoEstadoException("El bloque " + req.desde() + " a " + req.hasta()
                    + " se superpone con otro bloque de la máquina " + m.getId() + " (RN-13)");
        }
        return BloqueResponse.de(bloques.save(new BloqueDisponibilidad(m, req.desde(), req.hasta())));
    }

    @Transactional(readOnly = true)
    public AgendaResponse consultar(Long maquinaId) {
        Maquina m = maquinas.findWithContratistaById(maquinaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la máquina " + maquinaId));
        CurrentUser.verificarDuenio(m.getContratista().getSub(), "la máquina " + maquinaId);
        return new AgendaResponse(maquinaId,
                bloques.findByMaquinaIdOrderByDesdeAsc(maquinaId).stream().map(BloqueResponse::de).toList(),
                reservas.findByMaquinaIdOrderByDesdeAsc(maquinaId).stream().map(ReservaResponse::de).toList());
    }

    static void validarIntervalo(LocalDateTime desde, LocalDateTime hasta) {
        if (!desde.isBefore(hasta)) {
            throw new DatosInvalidosException("'desde' debe ser anterior a 'hasta'");
        }
    }
}
