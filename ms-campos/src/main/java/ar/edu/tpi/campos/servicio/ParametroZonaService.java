package ar.edu.tpi.campos.servicio;

import ar.edu.tpi.campos.dominio.CategoriaToxicologica;
import ar.edu.tpi.campos.dominio.ParametroZona;
import ar.edu.tpi.campos.repositorio.ParametroZonaRepository;
import ar.edu.tpi.campos.web.dto.ParametroZonaRequest;
import ar.edu.tpi.campos.web.dto.ParametroZonaResponse;
import ar.edu.tpi.commons.excepcion.DatosInvalidosException;
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

    /**
     * F-03: crea o actualiza los parámetros de una provincia. Valida RN-08:
     * 0 &lt; exclusión &lt; amortiguamiento y categorías existentes (si no, 400).
     */
    @Transactional
    public ParametroZonaResponse configurar(String provincia, ParametroZonaRequest req) {
        if (provincia == null || provincia.isBlank()) {
            throw new DatosInvalidosException("La provincia es obligatoria");
        }
        if (req.distanciaExclusionM() >= req.distanciaAmortiguamientoM()) {
            throw new DatosInvalidosException("La distancia de exclusión debe ser menor que la de amortiguamiento (RN-08)");
        }
        List<String> invalidas = req.categoriasPermitidas().stream()
                .filter(c -> CategoriaToxicologica.desdeCodigo(c).isEmpty()).toList();
        if (!invalidas.isEmpty()) {
            throw new DatosInvalidosException("Categorías toxicológicas inexistentes (RN-08): " + invalidas
                    + ". Válidas: Ia, Ib, II, III, IV");
        }
        List<String> categorias = req.categoriasPermitidas().stream()
                .map(c -> CategoriaToxicologica.desdeCodigo(c).orElseThrow())
                .distinct().sorted().map(CategoriaToxicologica::codigo).toList();

        ParametroZona p = repo.findByProvinciaIgnoreCase(provincia.trim())
                .orElseGet(() -> new ParametroZona(provincia.trim(), 0, 0, List.of()));
        p.setDistanciaExclusionM(req.distanciaExclusionM());
        p.setDistanciaAmortiguamientoM(req.distanciaAmortiguamientoM());
        p.setCategorias(categorias);
        return ParametroZonaResponse.de(repo.save(p));
    }

    /** Categorías permitidas en amortiguamiento; vacío si la provincia ya no tiene parámetros. */
    @Transactional(readOnly = true)
    public List<String> categoriasDe(String provincia) {
        return repo.findByProvinciaIgnoreCase(provincia).map(ParametroZona::getCategorias).orElse(List.of());
    }

    /** Parámetros de la provincia del lote; 422 si la provincia no está configurada (RN-05). */
    @Transactional(readOnly = true)
    public ParametroZona deProvincia(String provincia) {
        return repo.findByProvinciaIgnoreCase(provincia)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La provincia " + provincia + " no tiene parámetros de zona configurados (RN-05)"));
    }
}
