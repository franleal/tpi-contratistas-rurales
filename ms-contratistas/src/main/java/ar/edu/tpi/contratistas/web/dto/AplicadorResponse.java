package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.Aplicador;
import ar.edu.tpi.contratistas.dominio.Habilitacion;

import java.util.List;

public record AplicadorResponse(Long id, String nombre, String documento, DocumentoResumen carnet) {

    public static AplicadorResponse de(Aplicador a, List<Habilitacion> carnets) {
        return new AplicadorResponse(a.getId(), a.getNombre(), a.getDocumento(), DocumentoResumen.ultimo(carnets));
    }
}
