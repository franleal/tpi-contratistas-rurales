package ar.edu.tpi.contratistas.web;

import ar.edu.tpi.contratistas.servicio.ContratistaService;
import ar.edu.tpi.contratistas.web.dto.ContratistaRequest;
import ar.edu.tpi.contratistas.web.dto.ContratistaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-04. Rol CONTRATISTA controlado por el Gateway. */
@RestController
@RequestMapping("/api/contratistas")
public class ContratistaController {

    private final ContratistaService service;

    public ContratistaController(ContratistaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContratistaResponse registrar(@Valid @RequestBody ContratistaRequest req) {
        return service.registrar(req);
    }

    @GetMapping("/me")
    public ContratistaResponse propia() {
        return service.consultarPropia();
    }
}
