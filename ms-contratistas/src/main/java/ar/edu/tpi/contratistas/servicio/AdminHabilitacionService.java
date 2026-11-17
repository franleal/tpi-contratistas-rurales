package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.contratistas.dominio.EstadoHabilitacion;
import ar.edu.tpi.contratistas.dominio.Habilitacion;
import ar.edu.tpi.contratistas.repositorio.HabilitacionRepository;
import ar.edu.tpi.contratistas.web.dto.HabilitacionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** F-08: validación documental de carnets e inspecciones por el ADMIN. */
@Service
public class AdminHabilitacionService {

    private static final Logger log = LoggerFactory.getLogger(AdminHabilitacionService.class);

    private final HabilitacionRepository repo;

    public AdminHabilitacionService(HabilitacionRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<HabilitacionResponse> listar(EstadoHabilitacion estado) {
        List<Habilitacion> lista = estado == null ? repo.findAllByOrderByIdAsc() : repo.findByEstadoOrderByIdAsc(estado);
        return lista.stream().map(HabilitacionResponse::de).toList();
    }

    @Transactional
    public HabilitacionResponse aprobar(Long id) {
        Habilitacion h = buscar(id);
        h.aprobar();
        log.info("Habilitación {} ({}) VALIDADA", id, h.getTipo());
        return HabilitacionResponse.de(h);
    }

    @Transactional
    public HabilitacionResponse rechazar(Long id, String motivo) {
        Habilitacion h = buscar(id);
        h.rechazar(motivo.trim());
        log.info("Habilitación {} ({}) RECHAZADA: {}", id, h.getTipo(), motivo);
        return HabilitacionResponse.de(h);
    }

    private Habilitacion buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la habilitación " + id));
    }
}
