package ar.edu.tpi.trabajos.web.dto;

import ar.edu.tpi.trabajos.dominio.Receta;

import java.time.LocalDateTime;

public record RecetaResponse(Long id, String agronomo, String matricula, String producto,
                             String categoriaToxicologica, String dosis, LocalDateTime firmadaEn) {

    public static RecetaResponse de(Receta r) {
        return new RecetaResponse(r.getId(), r.getAgronomo().getNombre(), r.getAgronomo().getMatricula(),
                r.getProducto(), r.getCategoriaToxicologica(), r.getDosis(), r.getFirmadaEn());
    }
}
