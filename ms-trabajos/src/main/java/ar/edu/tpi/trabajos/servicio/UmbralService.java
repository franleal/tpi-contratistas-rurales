package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.DatosInvalidosException;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import ar.edu.tpi.trabajos.dominio.UmbralClimatico;
import ar.edu.tpi.trabajos.repositorio.UmbralClimaticoRepository;
import ar.edu.tpi.trabajos.web.dto.UmbralRequest;
import ar.edu.tpi.trabajos.web.dto.UmbralResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-09: fila única de umbrales climáticos y horizonte del pronóstico. */
@Service
public class UmbralService {

    private final UmbralClimaticoRepository repo;

    public UmbralService(UmbralClimaticoRepository repo) {
        this.repo = repo;
    }

    /** Umbrales vigentes; 422 si nunca se configuraron (data.sql los precarga). */
    @Transactional(readOnly = true)
    public UmbralClimatico vigentes() {
        return repo.findById(UmbralClimatico.ID)
                .orElseThrow(() -> new ReglaNegocioException("Los umbrales climáticos no están configurados (F-09)"));
    }

    @Transactional(readOnly = true)
    public UmbralResponse consultar() {
        return UmbralResponse.de(vigentes());
    }

    @Transactional
    public UmbralResponse configurar(UmbralRequest req) {
        if (req.vientoMinKmh().compareTo(req.vientoMaxKmh()) >= 0) {
            throw new DatosInvalidosException("El viento mínimo debe ser menor que el máximo");
        }
        if (req.rafagaMaxKmh().compareTo(req.vientoMaxKmh()) < 0) {
            throw new DatosInvalidosException("La ráfaga máxima no puede ser menor que el viento máximo");
        }
        UmbralClimatico u = repo.findById(UmbralClimatico.ID).orElseGet(UmbralClimatico::new);
        u.setVientoMinKmh(req.vientoMinKmh());
        u.setVientoMaxKmh(req.vientoMaxKmh());
        u.setRafagaMaxKmh(req.rafagaMaxKmh());
        u.setTemperaturaMaxC(req.temperaturaMaxC());
        u.setHumedadMinPct(req.humedadMinPct());
        u.setHorasSinLluviaDespues(req.horasSinLluviaDespues());
        u.setHorizonteDias(req.horizonteDias());
        return UmbralResponse.de(repo.save(u));
    }
}
