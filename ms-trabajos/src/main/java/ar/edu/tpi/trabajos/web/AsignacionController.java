package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.servicio.AsignacionService;
import ar.edu.tpi.trabajos.web.dto.AsignacionRequest;
import ar.edu.tpi.trabajos.web.dto.OrdenDetalleResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** F-15. Rol PRODUCTOR (Gateway); dueño de la orden (servicio). */
@RestController
public class AsignacionController {

    private final AsignacionService service;

    public AsignacionController(AsignacionService service) {
        this.service = service;
    }

    @PostMapping("/api/ordenes/{id}/asignacion")
    public OrdenDetalleResponse asignar(@PathVariable Long id, @Valid @RequestBody AsignacionRequest req) {
        return service.asignar(id, req);
    }
}
