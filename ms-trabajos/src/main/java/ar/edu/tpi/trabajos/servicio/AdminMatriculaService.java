package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.trabajos.dominio.Agronomo;
import ar.edu.tpi.trabajos.dominio.EstadoMatricula;
import ar.edu.tpi.trabajos.repositorio.AgronomoRepository;
import ar.edu.tpi.trabajos.web.dto.AgronomoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** F-08: validación documental de matrículas. */
@Service
public class AdminMatriculaService {

    private final AgronomoRepository repo;

    public AdminMatriculaService(AgronomoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<AgronomoResponse> listar(EstadoMatricula estado) {
        var lista = estado == null ? repo.findAllByOrderByIdAsc() : repo.findByEstadoOrderByIdAsc(estado);
        return lista.stream().map(AgronomoResponse::de).toList();
    }

    @Transactional
    public AgronomoResponse aprobar(Long id) {
        Agronomo a = buscar(id);
        a.aprobar();
        return AgronomoResponse.de(a);
    }

    @Transactional
    public AgronomoResponse rechazar(Long id, String motivo) {
        Agronomo a = buscar(id);
        a.rechazar(motivo.trim());
        return AgronomoResponse.de(a);
    }

    private Agronomo buscar(Long id) {
        return repo.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la matrícula " + id));
    }
}
