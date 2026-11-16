package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.servicio.ConsultaOrdenService;
import ar.edu.tpi.trabajos.servicio.OrdenService;
import ar.edu.tpi.trabajos.web.dto.OrdenDetalleResponse;
import ar.edu.tpi.trabajos.web.dto.OrdenRequest;
import ar.edu.tpi.trabajos.web.dto.OrdenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-10 y consultas. Roles por ruta en el Gateway; propiedad y filtrado por rol en el servicio. */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService service;
    private final ConsultaOrdenService consultas;

    public OrdenController(OrdenService service, ConsultaOrdenService consultas) {
        this.service = service;
        this.consultas = consultas;
    }

    @GetMapping
    public List<OrdenResponse> listar(@RequestParam(required = false) EstadoOrden estado) {
        return consultas.listar(estado);
    }

    @GetMapping("/{id}")
    public OrdenDetalleResponse detalle(@PathVariable Long id) {
        return consultas.detalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrdenResponse crear(@Valid @RequestBody OrdenRequest req) {
        return service.crear(req);
    }
}
