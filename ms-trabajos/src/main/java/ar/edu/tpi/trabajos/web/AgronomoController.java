package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.servicio.AgronomoService;
import ar.edu.tpi.trabajos.web.dto.AgronomoRequest;
import ar.edu.tpi.trabajos.web.dto.AgronomoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-07. Rol AGRONOMO controlado por el Gateway. */
@RestController
@RequestMapping("/api/agronomos")
public class AgronomoController {

    private final AgronomoService service;

    public AgronomoController(AgronomoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgronomoResponse registrar(@Valid @RequestBody AgronomoRequest req) {
        return service.registrar(req);
    }

    @GetMapping("/me")
    public AgronomoResponse propio() {
        return service.propio();
    }
}
