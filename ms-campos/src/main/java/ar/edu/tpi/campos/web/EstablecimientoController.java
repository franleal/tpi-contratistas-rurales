package ar.edu.tpi.campos.web;

import ar.edu.tpi.campos.servicio.EstablecimientoService;
import ar.edu.tpi.campos.web.dto.EstablecimientoRequest;
import ar.edu.tpi.campos.web.dto.EstablecimientoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-01. Rol PRODUCTOR controlado por el Gateway; propiedad por sub en el servicio. */
@RestController
@RequestMapping("/api/establecimientos")
public class EstablecimientoController {

    private final EstablecimientoService service;

    public EstablecimientoController(EstablecimientoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstablecimientoResponse crear(@Valid @RequestBody EstablecimientoRequest req) {
        return service.crear(req);
    }

    @GetMapping
    public List<EstablecimientoResponse> listarPropios() {
        return service.listarPropios();
    }
}
