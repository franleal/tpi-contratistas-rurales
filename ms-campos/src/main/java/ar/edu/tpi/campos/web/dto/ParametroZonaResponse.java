package ar.edu.tpi.campos.web.dto;

import ar.edu.tpi.campos.dominio.ParametroZona;

import java.util.List;

public record ParametroZonaResponse(String provincia, int distanciaExclusionM, int distanciaAmortiguamientoM,
                                    List<String> categoriasPermitidas) {

    public static ParametroZonaResponse de(ParametroZona p) {
        return new ParametroZonaResponse(p.getProvincia(), p.getDistanciaExclusionM(),
                p.getDistanciaAmortiguamientoM(), p.getCategorias());
    }
}
