package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.servicio.PostulacionService;
import ar.edu.tpi.trabajos.web.dto.PostulacionRequest;
import ar.edu.tpi.trabajos.web.dto.PostulacionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** F-13 (CONTRATISTA dueño de la máquina) y F-14 (PRODUCTOR dueño de la orden). */
@RestController
@RequestMapping("/api/ordenes/{id}/postulaciones")
public class PostulacionController {

    private final PostulacionService service;

    public PostulacionController(PostulacionService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostulacionResponse postularse(@PathVariable Long id, @Valid @RequestBody PostulacionRequest req) {
        return service.postularse(id, req);
    }
}
