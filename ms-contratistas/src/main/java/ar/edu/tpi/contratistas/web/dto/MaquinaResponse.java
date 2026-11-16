package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.Habilitacion;
import ar.edu.tpi.contratistas.dominio.Maquina;
import ar.edu.tpi.contratistas.dominio.TipoMaquina;

import java.math.BigDecimal;
import java.util.List;

public record MaquinaResponse(Long id, TipoMaquina tipo, String descripcion, BigDecimal anchoM,
                              BigDecimal rendimientoHaH, BigDecimal baseLat, BigDecimal baseLon,
                              DocumentoResumen inspeccion) {

    public static MaquinaResponse de(Maquina m, List<Habilitacion> inspecciones) {
        return new MaquinaResponse(m.getId(), m.getTipo(), m.getDescripcion(), m.getAnchoM(), m.getRendimientoHaH(),
                m.getBaseLat(), m.getBaseLon(), DocumentoResumen.ultimo(inspecciones));
    }
}
