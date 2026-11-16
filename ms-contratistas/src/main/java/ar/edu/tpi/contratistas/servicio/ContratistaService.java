package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.contratistas.dominio.Contratista;
import ar.edu.tpi.contratistas.repositorio.ContratistaRepository;
import ar.edu.tpi.contratistas.web.dto.ContratistaRequest;
import ar.edu.tpi.contratistas.web.dto.ContratistaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-04: registro de la empresa contratista vinculada al sub del token. */
@Service
public class ContratistaService {

    private final ContratistaRepository repo;

    public ContratistaService(ContratistaRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ContratistaResponse registrar(ContratistaRequest req) {
        String sub = CurrentUser.sub();
        if (repo.findBySub(sub).isPresent()) {
            throw new ConflictoEstadoException("El usuario ya registró su empresa contratista");
        }
        String cuit = req.cuit().replace("-", "");
        if (repo.existsByCuit(cuit)) {
            throw new ConflictoEstadoException("Ya existe una empresa con el CUIT " + req.cuit());
        }
        return ContratistaResponse.de(repo.save(new Contratista(sub, req.razonSocial().trim(), cuit)));
    }

    @Transactional(readOnly = true)
    public ContratistaResponse consultarPropia() {
        return ContratistaResponse.de(actual());
    }

    /** Empresa del usuario actual; 422 si todavía no la registró. */
    @Transactional(readOnly = true)
    public Contratista actual() {
        return repo.findBySub(CurrentUser.sub())
                .orElseThrow(() -> new ReglaNegocioException(
                        "Primero debe registrar su empresa contratista (POST /api/contratistas)"));
    }
}
