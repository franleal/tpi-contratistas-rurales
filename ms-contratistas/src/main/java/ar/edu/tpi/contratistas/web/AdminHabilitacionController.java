package ar.edu.tpi.contratistas.web;

import ar.edu.tpi.contratistas.dominio.EstadoHabilitacion;
import ar.edu.tpi.contratistas.servicio.AdminHabilitacionService;
import ar.edu.tpi.contratistas.web.dto.HabilitacionResponse;
import ar.edu.tpi.contratistas.web.dto.RechazoRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-08. Rol ADMIN controlado por el Gateway. */
@RestController
@RequestMapping("/api/admin/habilitaciones")
public class AdminHabilitacionController {

    private final AdminHabilitacionService service;

    public AdminHabilitacionController(AdminHabilitacionService service) {
        this.service = service;
    }

    @GetMapping
    public List<HabilitacionResponse> listar(@RequestParam(required = false) EstadoHabilitacion estado) {
        return service.listar(estado);
    }

    @PostMapping("/{id}/aprobar")
    public HabilitacionResponse aprobar(@PathVariable Long id) {
        return service.aprobar(id);
    }

    @PostMapping("/{id}/rechazar")
    public HabilitacionResponse rechazar(@PathVariable Long id, @Valid @RequestBody RechazoRequest req) {
        return service.rechazar(id, req.motivo());
    }
}
