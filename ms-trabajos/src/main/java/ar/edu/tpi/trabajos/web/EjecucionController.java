package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.servicio.EjecucionService;
import ar.edu.tpi.trabajos.web.dto.EjecucionRequest;
import ar.edu.tpi.trabajos.web.dto.OrdenDetalleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-16. Rol CONTRATISTA (Gateway); contratista asignado (servicio). */
@RestController
public class EjecucionController {

    private final EjecucionService service;

    public EjecucionController(EjecucionService service) {
        this.service = service;
    }

    @PostMapping("/api/ordenes/{id}/ejecucion")
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenDetalleResponse registrar(@PathVariable Long id, @Valid @RequestBody EjecucionRequest req) {
        return service.registrar(id, req);
    }
}
