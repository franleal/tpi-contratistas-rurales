package ar.edu.tpi.contratistas.web;

import ar.edu.tpi.contratistas.servicio.AgendaService;
import ar.edu.tpi.contratistas.web.dto.AgendaResponse;
import ar.edu.tpi.contratistas.web.dto.BloqueRequest;
import ar.edu.tpi.contratistas.web.dto.BloqueResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-06. Rol CONTRATISTA (Gateway); dueño de la máquina (servicio). */
@RestController
@RequestMapping("/api/agenda/bloques")
public class AgendaController {

    private final AgendaService service;

    public AgendaController(AgendaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueResponse declarar(@Valid @RequestBody BloqueRequest req) {
        return service.declarar(req);
    }

    @GetMapping
    public AgendaResponse consultar(@RequestParam Long maquinaId) {
        return service.consultar(maquinaId);
    }
}
