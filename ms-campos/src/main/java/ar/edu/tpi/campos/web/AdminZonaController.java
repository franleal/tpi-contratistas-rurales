package ar.edu.tpi.campos.web;

import ar.edu.tpi.campos.servicio.ParametroZonaService;
import ar.edu.tpi.campos.web.dto.ParametroZonaRequest;
import ar.edu.tpi.campos.web.dto.ParametroZonaResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-03. Rol ADMIN controlado por el Gateway. */
@RestController
@RequestMapping("/api/admin/zonas")
public class AdminZonaController {

    private final ParametroZonaService service;

    public AdminZonaController(ParametroZonaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ParametroZonaResponse> listar() {
        return service.listar();
    }

    @PutMapping("/{provincia}")
    public ParametroZonaResponse configurar(@PathVariable String provincia, @Valid @RequestBody ParametroZonaRequest req) {
        return service.configurar(provincia, req);
    }
}
