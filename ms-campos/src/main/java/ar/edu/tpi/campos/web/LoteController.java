package ar.edu.tpi.campos.web;

import ar.edu.tpi.campos.servicio.LoteService;
import ar.edu.tpi.campos.web.dto.LoteRequest;
import ar.edu.tpi.campos.web.dto.LoteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-02. Rol PRODUCTOR (Gateway); dueño del establecimiento (servicio). */
@RestController
@RequestMapping("/api/lotes")
public class LoteController {

    private final LoteService service;

    public LoteController(LoteService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public LoteResponse consultar(@PathVariable Long id) {
        return service.consultarPropio(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoteResponse registrar(@Valid @RequestBody LoteRequest req) {
        return service.registrar(req);
    }
}
