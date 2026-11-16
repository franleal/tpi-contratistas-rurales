package ar.edu.tpi.contratistas.web;

import ar.edu.tpi.contratistas.servicio.ReservaService;
import ar.edu.tpi.contratistas.web.dto.ReservaRequest;
import ar.edu.tpi.contratistas.web.dto.ReservaResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Reservas de agenda: solo las invoca ms-trabajos por la red interna. */
@RestController
@RequestMapping("/internal/agenda/reservas")
public class InternalAgendaController {

    private final ReservaService service;

    public InternalAgendaController(ReservaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse reservar(@Valid @RequestBody ReservaRequest req) {
        return service.reservar(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void liberar(@PathVariable Long id) {
        service.liberar(id);
    }
}
