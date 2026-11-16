package ar.edu.tpi.contratistas.web;

import ar.edu.tpi.contratistas.servicio.AplicadorService;
import ar.edu.tpi.contratistas.web.dto.AplicadorRequest;
import ar.edu.tpi.contratistas.web.dto.AplicadorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-04. Rol CONTRATISTA (Gateway); solo los propios (servicio). */
@RestController
@RequestMapping("/api/aplicadores")
public class AplicadorController {

    private final AplicadorService service;

    public AplicadorController(AplicadorService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AplicadorResponse registrar(@Valid @RequestBody AplicadorRequest req) {
        return service.registrar(req);
    }

    @GetMapping
    public List<AplicadorResponse> listarPropios() {
        return service.listarPropios();
    }
}
