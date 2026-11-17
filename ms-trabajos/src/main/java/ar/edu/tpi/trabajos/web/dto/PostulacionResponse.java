package ar.edu.tpi.trabajos.web.dto;

import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.dominio.Postulacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PostulacionResponse(Long id, Long ordenId, String contratistaSub, Long maquinaId, Long aplicadorId,
                                  LocalDateTime inicio, LocalDateTime fin, LocalDateTime ocupadoDesde,
                                  BigDecimal horasAplicacion, BigDecimal trasladoH, BigDecimal precioHa,
                                  EstadoPostulacion estado) {

    public static PostulacionResponse de(Postulacion p) {
        return new PostulacionResponse(p.getId(), p.getOrden().getId(), p.getContratistaSub(), p.getMaquinaId(),
                p.getAplicadorId(), p.getInicio(), p.getFin(), p.getOcupadoDesde(), p.getHorasAplicacion(),
                p.getTrasladoH(), p.getPrecioHa(), p.getEstado());
    }
}
