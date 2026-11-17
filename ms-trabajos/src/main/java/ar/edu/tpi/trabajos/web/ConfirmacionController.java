package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.servicio.ConfirmacionService;
import ar.edu.tpi.trabajos.web.dto.OrdenDetalleResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** F-17. Rol PRODUCTOR (Gateway); dueño de la orden (servicio). */
@RestController
public class ConfirmacionController {

    private final ConfirmacionService service;

    public ConfirmacionController(ConfirmacionService service) {
        this.service = service;
    }

    @PostMapping("/api/ordenes/{id}/confirmacion")
    public OrdenDetalleResponse confirmar(@PathVariable Long id) {
        return service.confirmar(id);
    }
}
