package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.contratistas.dominio.Aplicador;
import ar.edu.tpi.contratistas.dominio.Maquina;
import ar.edu.tpi.contratistas.repositorio.HabilitacionRepository;
import ar.edu.tpi.contratistas.servicio.EvaluadorHabilitacion.Resultado;
import ar.edu.tpi.contratistas.web.dto.HabilitacionInternaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Evaluación de RN-02 a RN-04 para ms-trabajos (postulación y revalidación al asignar). */
@Service
public class HabilitacionInternaService {

    private final MaquinaService maquinas;
    private final AplicadorService aplicadores;
    private final HabilitacionRepository habilitaciones;
    private final EvaluadorHabilitacion evaluador;

    public HabilitacionInternaService(MaquinaService maquinas, AplicadorService aplicadores,
                                      HabilitacionRepository habilitaciones, EvaluadorHabilitacion evaluador) {
        this.maquinas = maquinas;
        this.aplicadores = aplicadores;
        this.habilitaciones = habilitaciones;
        this.evaluador = evaluador;
    }

    @Transactional(readOnly = true)
    public HabilitacionInternaResponse evaluar(Long maquinaId, Long aplicadorId, LocalDate fecha) {
        Maquina m = maquinas.buscar(maquinaId);
        Aplicador a = aplicadores.buscar(aplicadorId);
        Resultado r = evaluador.evaluar(m, habilitaciones.findByMaquinaId(maquinaId),
                a, habilitaciones.findByAplicadorId(aplicadorId), fecha);
        return new HabilitacionInternaResponse(maquinaId, aplicadorId, fecha, r.habilitado(), r.motivo(), m.getTipo(),
                m.getRendimientoHaH(), m.getBaseLat(), m.getBaseLon(), m.getContratista().getSub());
    }
}
