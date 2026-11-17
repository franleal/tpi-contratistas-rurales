package ar.edu.tpi.campos.web;

import ar.edu.tpi.campos.servicio.LoteService;
import ar.edu.tpi.campos.web.dto.LoteInternoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rutas internas: no se publican en el Gateway. Exigen un JWT válido (propagado por
 * ms-trabajos) pero no un rol.
 */
@RestController
@RequestMapping("/internal/lotes")
public class InternalLoteController {

    private final LoteService service;

    public InternalLoteController(LoteService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public LoteInternoResponse consultar(@PathVariable Long id) {
        return service.consultarInterno(id);
    }
}
