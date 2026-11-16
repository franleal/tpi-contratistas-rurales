package ar.edu.tpi.campos.servicio;

import ar.edu.tpi.campos.dominio.ParametroZona;
import ar.edu.tpi.campos.repositorio.ParametroZonaRepository;
import ar.edu.tpi.campos.web.dto.ParametroZonaResponse;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** F-03: parámetros de zona por provincia (RN-08). */
@Service
public class ParametroZonaService {

    private final ParametroZonaRepository repo;

    public ParametroZonaService(ParametroZonaRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ParametroZonaResponse> listar() {
        return repo.findAll().stream()
                .sorted((a, b) -> a.getProvincia().compareToIgnoreCase(b.getProvincia()))
                .map(ParametroZonaResponse::de)
                .toList();
    }

    /** Parámetros de la provincia del lote; 422 si la provincia no está configurada (RN-05). */
    @Transactional(readOnly = true)
    public ParametroZona deProvincia(String provincia) {
        return repo.findByProvinciaIgnoreCase(provincia)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La provincia " + provincia + " no tiene parámetros de zona configurados (RN-05)"));
    }
}
