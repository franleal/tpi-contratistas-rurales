package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.trabajos.dominio.Agronomo;
import ar.edu.tpi.trabajos.repositorio.AgronomoRepository;
import ar.edu.tpi.trabajos.web.dto.AgronomoRequest;
import ar.edu.tpi.trabajos.web.dto.AgronomoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-07: matrícula del agrónomo del token, en PENDIENTE hasta que la valide el ADMIN. */
@Service
public class AgronomoService {

    private final AgronomoRepository repo;

    public AgronomoService(AgronomoRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public AgronomoResponse registrar(AgronomoRequest req) {
        String sub = CurrentUser.sub();
        if (repo.findBySub(sub).isPresent()) {
            throw new ConflictoEstadoException("El agrónomo ya registró su matrícula");
        }
        Agronomo a = new Agronomo();
        a.setSub(sub);
        a.setNombre(CurrentUser.nombre());
        a.setMatricula(req.matricula().trim());
        a.setProvincia(req.provincia().trim());
        a.setVencimiento(req.vencimiento());
        return AgronomoResponse.de(repo.save(a));
    }

    @Transactional(readOnly = true)
    public AgronomoResponse propio() {
        return repo.findBySub(CurrentUser.sub()).map(AgronomoResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("El agrónomo no registró su matrícula"));
    }
}
