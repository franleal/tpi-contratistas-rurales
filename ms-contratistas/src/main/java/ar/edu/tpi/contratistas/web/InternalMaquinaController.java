package ar.edu.tpi.contratistas.web;

import ar.edu.tpi.contratistas.servicio.HabilitacionInternaService;
import ar.edu.tpi.contratistas.web.dto.HabilitacionInternaResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Rutas internas (no publicadas en el Gateway): token válido, sin rol. */
@RestController
@RequestMapping("/internal/maquinas")
public class InternalMaquinaController {

    private final HabilitacionInternaService habilitacion;

    public InternalMaquinaController(HabilitacionInternaService habilitacion) {
        this.habilitacion = habilitacion;
    }

    @GetMapping("/{id}/habilitacion")
    public HabilitacionInternaResponse habilitacion(@PathVariable Long id, @RequestParam Long aplicadorId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                    LocalDate fecha) {
        return habilitacion.evaluar(id, aplicadorId, fecha);
    }
}
