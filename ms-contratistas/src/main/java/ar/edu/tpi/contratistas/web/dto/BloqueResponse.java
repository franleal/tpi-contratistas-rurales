package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.BloqueDisponibilidad;

import java.time.LocalDateTime;

public record BloqueResponse(Long id, Long maquinaId, LocalDateTime desde, LocalDateTime hasta) {

    public static BloqueResponse de(BloqueDisponibilidad b) {
        return new BloqueResponse(b.getId(), b.getMaquina().getId(), b.getDesde(), b.getHasta());
    }
}
