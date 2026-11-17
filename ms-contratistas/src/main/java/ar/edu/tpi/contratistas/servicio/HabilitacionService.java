package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.contratistas.dominio.Habilitacion;
import ar.edu.tpi.contratistas.dominio.TipoMaquina;
import ar.edu.tpi.contratistas.repositorio.HabilitacionRepository;
import ar.edu.tpi.contratistas.web.dto.HabilitacionRequest;
import ar.edu.tpi.contratistas.web.dto.HabilitacionResponse;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-05: carga de carnets e inspecciones de recursos propios, siempre en PENDIENTE. */
@Service
public class HabilitacionService {

    private final HabilitacionRepository repo;
    private final MaquinaService maquinas;
    private final AplicadorService aplicadores;

    public HabilitacionService(HabilitacionRepository repo, MaquinaService maquinas, AplicadorService aplicadores) {
        this.repo = repo;
        this.maquinas = maquinas;
        this.aplicadores = aplicadores;
    }

    @Transactional
    public HabilitacionResponse cargar(HabilitacionRequest req) {
        String numero = req.numero().trim();
        Habilitacion h = switch (req.tipo()) {
            case CARNET_APLICADOR -> Habilitacion.carnet(aplicadores.propio(req.titularId()), numero, req.vencimiento());
            case INSPECCION_MAQUINA -> {
                var m = maquinas.propia(req.titularId());
                if (m.getTipo() != TipoMaquina.PULVERIZADORA) {
                    throw new ReglaNegocioException("Solo una PULVERIZADORA requiere inspección técnica (RN-03)");
                }
                yield Habilitacion.inspeccion(m, numero, req.vencimiento());
            }
        };
        return HabilitacionResponse.de(repo.save(h));
    }
}
