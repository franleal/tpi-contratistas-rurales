package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.Contratista;

public record ContratistaResponse(Long id, String razonSocial, String cuit) {

    public static ContratistaResponse de(Contratista c) {
        String cuit = c.getCuit();
        return new ContratistaResponse(c.getId(), c.getRazonSocial(),
                cuit.substring(0, 2) + "-" + cuit.substring(2, 10) + "-" + cuit.substring(10));
    }
}
