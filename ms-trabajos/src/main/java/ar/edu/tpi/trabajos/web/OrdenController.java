package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.servicio.OrdenService;
import ar.edu.tpi.trabajos.web.dto.OrdenRequest;
import ar.edu.tpi.trabajos.web.dto.OrdenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-10. Rol PRODUCTOR (Gateway); dueño del lote (servicio). */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService service;

    public OrdenController(OrdenService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenResponse crear(@Valid @RequestBody OrdenRequest req) {
        return service.crear(req);
    }
}
