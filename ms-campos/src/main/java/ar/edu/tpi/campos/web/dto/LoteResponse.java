package ar.edu.tpi.campos.web.dto;

import ar.edu.tpi.campos.dominio.Lote;
import ar.edu.tpi.campos.dominio.Zona;

import java.math.BigDecimal;

public record LoteResponse(Long id, Long establecimientoId, String nombre, BigDecimal lat, BigDecimal lon,
                           BigDecimal superficieHa, String cultivo, String provincia, String departamento,
                           String localidadCercana, Integer distanciaLocalidadM, Zona zona) {

    public static LoteResponse de(Lote l) {
        return new LoteResponse(l.getId(), l.getEstablecimiento().getId(), l.getNombre(), l.getLat(), l.getLon(),
                l.getSuperficieHa(), l.getCultivo(), l.getProvincia(), l.getDepartamento(), l.getLocalidadCercana(),
                l.getDistanciaLocalidadM(), l.getZona());
    }
}
