package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.servicio.RecetaService;
import ar.edu.tpi.trabajos.web.dto.OrdenDetalleResponse;
import ar.edu.tpi.trabajos.web.dto.RecetaRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-11. Rol AGRONOMO (Gateway); matrícula validada (servicio). */
@RestController
public class RecetaController {

    private final RecetaService service;

    public RecetaController(RecetaService service) {
        this.service = service;
    }

    @PostMapping("/api/ordenes/{id}/receta")
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenDetalleResponse firmar(@PathVariable Long id, @Valid @RequestBody RecetaRequest req) {
        return service.firmar(id, req);
    }
}
