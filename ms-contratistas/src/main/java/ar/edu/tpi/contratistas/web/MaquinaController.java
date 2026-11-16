package ar.edu.tpi.contratistas.web;

import ar.edu.tpi.contratistas.servicio.MaquinaService;
import ar.edu.tpi.contratistas.web.dto.MaquinaRequest;
import ar.edu.tpi.contratistas.web.dto.MaquinaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-04. Rol CONTRATISTA (Gateway); solo las propias (servicio). */
@RestController
@RequestMapping("/api/maquinas")
public class MaquinaController {

    private final MaquinaService service;

    public MaquinaController(MaquinaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaquinaResponse registrar(@Valid @RequestBody MaquinaRequest req) {
        return service.registrar(req);
    }

    @GetMapping
    public List<MaquinaResponse> listarPropias() {
        return service.listarPropias();
    }
}
