package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.contratistas.dominio.Maquina;
import ar.edu.tpi.contratistas.dominio.TipoMaquina;
import ar.edu.tpi.contratistas.repositorio.HabilitacionRepository;
import ar.edu.tpi.contratistas.repositorio.MaquinaRepository;
import ar.edu.tpi.contratistas.web.dto.MaquinaRequest;
import ar.edu.tpi.contratistas.web.dto.MaquinaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

/** F-04: máquinas del contratista autenticado. */
@Service
public class MaquinaService {

    private final MaquinaRepository maquinas;
    private final HabilitacionRepository habilitaciones;
    private final ContratistaService contratistas;

    public MaquinaService(MaquinaRepository maquinas, HabilitacionRepository habilitaciones,
                          ContratistaService contratistas) {
        this.maquinas = maquinas;
        this.habilitaciones = habilitaciones;
        this.contratistas = contratistas;
    }

    @Transactional
    public MaquinaResponse registrar(MaquinaRequest req) {
        Maquina m = new Maquina();
        m.setContratista(contratistas.actual());
        m.setTipo(req.tipo() == null ? TipoMaquina.PULVERIZADORA : req.tipo());
        m.setDescripcion(req.descripcion().trim());
        m.setAnchoM(req.anchoM().setScale(2, RoundingMode.HALF_UP));
        m.setRendimientoHaH(req.rendimientoHaH().setScale(2, RoundingMode.HALF_UP));
        m.setBaseLat(req.baseLat().setScale(6, RoundingMode.HALF_UP));
        m.setBaseLon(req.baseLon().setScale(6, RoundingMode.HALF_UP));
        return MaquinaResponse.de(maquinas.save(m), List.of());
    }

    @Transactional(readOnly = true)
    public List<MaquinaResponse> listarPropias() {
        return maquinas.findByContratistaSubOrderByIdAsc(CurrentUser.sub()).stream()
                .map(m -> MaquinaResponse.de(m, habilitaciones.findByMaquinaId(m.getId())))
                .toList();
    }

    /** Máquina existente (404), sin aplicar propiedad. */
    @Transactional(readOnly = true)
    public Maquina buscar(Long id) {
        return maquinas.findWithContratistaById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la máquina " + id));
    }

    /** Máquina propia: 404 si no existe, 403 si es de otro contratista. */
    @Transactional(readOnly = true)
    public Maquina propia(Long id) {
        Maquina m = buscar(id);
        CurrentUser.verificarDuenio(m.getContratista().getSub(), "la máquina " + id);
        return m;
    }
}
