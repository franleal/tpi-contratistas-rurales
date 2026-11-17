package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.contratistas.dominio.Aplicador;
import ar.edu.tpi.contratistas.dominio.Contratista;
import ar.edu.tpi.contratistas.repositorio.AplicadorRepository;
import ar.edu.tpi.contratistas.repositorio.HabilitacionRepository;
import ar.edu.tpi.contratistas.web.dto.AplicadorRequest;
import ar.edu.tpi.contratistas.web.dto.AplicadorResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** F-04: plantilla de aplicadores del contratista autenticado. */
@Service
public class AplicadorService {

    private final AplicadorRepository aplicadores;
    private final HabilitacionRepository habilitaciones;
    private final ContratistaService contratistas;

    public AplicadorService(AplicadorRepository aplicadores, HabilitacionRepository habilitaciones,
                            ContratistaService contratistas) {
        this.aplicadores = aplicadores;
        this.habilitaciones = habilitaciones;
        this.contratistas = contratistas;
    }

    @Transactional
    public AplicadorResponse registrar(AplicadorRequest req) {
        Contratista c = contratistas.actual();
        if (aplicadores.existsByContratistaIdAndDocumento(c.getId(), req.documento())) {
            throw new ConflictoEstadoException("Ya existe un aplicador con documento " + req.documento());
        }
        Aplicador a = aplicadores.save(new Aplicador(c, req.nombre().trim(), req.documento()));
        return AplicadorResponse.de(a, List.of());
    }

    @Transactional(readOnly = true)
    public List<AplicadorResponse> listarPropios() {
        return aplicadores.findByContratistaSubOrderByIdAsc(CurrentUser.sub()).stream()
                .map(a -> AplicadorResponse.de(a, habilitaciones.findByAplicadorId(a.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Aplicador buscar(Long id) {
        return aplicadores.findWithContratistaById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el aplicador " + id));
    }

    /** Aplicador propio: 404 si no existe, 403 si es de otro contratista. */
    @Transactional(readOnly = true)
    public Aplicador propio(Long id) {
        Aplicador a = buscar(id);
        CurrentUser.verificarDuenio(a.getContratista().getSub(), "el aplicador " + id);
        return a;
    }
}
