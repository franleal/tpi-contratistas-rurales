package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.servicio.VentanaService;
import ar.edu.tpi.trabajos.web.dto.VentanaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-12. Roles PRODUCTOR (dueño) y CONTRATISTA. */
@RestController
public class VentanaController {

    private final VentanaService service;

    public VentanaController(VentanaService service) {
        this.service = service;
    }

    @GetMapping("/api/ordenes/{id}/ventanas")
    public List<VentanaResponse> ventanas(@PathVariable Long id) {
        return service.ventanas(id);
    }
}
