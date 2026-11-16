package ar.edu.tpi.trabajos.web;

import ar.edu.tpi.trabajos.dominio.EstadoMatricula;
import ar.edu.tpi.trabajos.servicio.AdminMatriculaService;
import ar.edu.tpi.trabajos.web.dto.AgronomoResponse;
import ar.edu.tpi.trabajos.web.dto.RechazoRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** F-08. Rol ADMIN controlado por el Gateway. El id es el del agrónomo (una matrícula por agrónomo). */
@RestController
@RequestMapping("/api/admin/matriculas")
public class AdminMatriculaController {

    private final AdminMatriculaService service;

    public AdminMatriculaController(AdminMatriculaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AgronomoResponse> listar(@RequestParam(required = false) EstadoMatricula estado) {
        return service.listar(estado);
    }

    @PostMapping("/{id}/aprobar")
    public AgronomoResponse aprobar(@PathVariable Long id) {
        return service.aprobar(id);
    }

    @PostMapping("/{id}/rechazar")
    public AgronomoResponse rechazar(@PathVariable Long id, @Valid @RequestBody RechazoRequest req) {
        return service.rechazar(id, req.motivo());
    }
}
