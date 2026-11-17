package ar.edu.tpi.trabajos.web.dto;

import ar.edu.tpi.trabajos.dominio.Asignacion;
import ar.edu.tpi.trabajos.dominio.Postulacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AsignacionResponse(Long id, Long postulacionId, String contratistaSub, Long maquinaId,
                                 Long aplicadorId, LocalDateTime ocupadoDesde, LocalDateTime inicio,
                                 LocalDateTime fin, BigDecimal precioHa, Long reservaId, LocalDateTime asignadaEn) {

    public static AsignacionResponse de(Asignacion a) {
        Postulacion p = a.getPostulacion();
        return new AsignacionResponse(a.getId(), p.getId(), p.getContratistaSub(), p.getMaquinaId(),
                p.getAplicadorId(), p.getOcupadoDesde(), p.getInicio(), p.getFin(), p.getPrecioHa(),
                a.getReservaId(), a.getAsignadaEn());
    }
}
