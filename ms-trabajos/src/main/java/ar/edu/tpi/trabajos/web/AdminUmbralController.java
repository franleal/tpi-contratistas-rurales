package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.servicio.UmbralService;
import ar.edu.tpi.trabajos.web.dto.UmbralRequest;
import ar.edu.tpi.trabajos.web.dto.UmbralResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** F-09. Rol ADMIN controlado por el Gateway. */
@RestController
@RequestMapping("/api/admin/umbrales")
public class AdminUmbralController {

    private final UmbralService service;

    public AdminUmbralController(UmbralService service) {
        this.service = service;
    }

    @GetMapping
    public UmbralResponse consultar() {
        return service.consultar();
    }

    @PutMapping
    public UmbralResponse configurar(@Valid @RequestBody UmbralRequest req) {
        return service.configurar(req);
    }
}
