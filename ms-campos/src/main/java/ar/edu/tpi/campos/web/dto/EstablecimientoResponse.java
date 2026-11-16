package ar.edu.tpi.campos.web.dto;

import ar.edu.tpi.campos.dominio.Establecimiento;
import ar.edu.tpi.campos.dominio.Lote;
import ar.edu.tpi.campos.dominio.Zona;

import java.math.BigDecimal;
import java.util.List;

public record EstablecimientoResponse(Long id, String nombre, String provincia, List<LoteResumen> lotes) {

    public record LoteResumen(Long id, String nombre, BigDecimal superficieHa, String cultivo, Zona zona) {
        static LoteResumen de(Lote l) {
            return new LoteResumen(l.getId(), l.getNombre(), l.getSuperficieHa(), l.getCultivo(), l.getZona());
        }
    }

    public static EstablecimientoResponse de(Establecimiento e) {
        return new EstablecimientoResponse(e.getId(), e.getNombre(), e.getProvincia(),
                e.getLotes().stream().map(LoteResumen::de).toList());
    }
}
