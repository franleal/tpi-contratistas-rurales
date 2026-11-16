package ar.edu.tpi.contratistas.web;

import ar.edu.tpi.contratistas.servicio.HabilitacionService;
import ar.edu.tpi.contratistas.web.dto.HabilitacionRequest;
import ar.edu.tpi.contratistas.web.dto.HabilitacionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-05. Rol CONTRATISTA (Gateway); dueño del titular (servicio). */
@RestController
@RequestMapping("/api/habilitaciones")
public class HabilitacionController {

    private final HabilitacionService service;

    public HabilitacionController(HabilitacionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HabilitacionResponse cargar(@Valid @RequestBody HabilitacionRequest req) {
        return service.cargar(req);
    }
}
