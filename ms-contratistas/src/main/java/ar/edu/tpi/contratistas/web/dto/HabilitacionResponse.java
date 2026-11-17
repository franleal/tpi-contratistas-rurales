package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.EstadoHabilitacion;
import ar.edu.tpi.contratistas.dominio.Habilitacion;
import ar.edu.tpi.contratistas.dominio.TipoHabilitacion;

import java.time.LocalDate;

public record HabilitacionResponse(Long id, TipoHabilitacion tipo, Long titularId, String titular, String numero,
                                   LocalDate vencimiento, EstadoHabilitacion estado, String motivoRechazo) {

    public static HabilitacionResponse de(Habilitacion h) {
        boolean carnet = h.getTipo() == TipoHabilitacion.CARNET_APLICADOR;
        Long titularId = carnet ? h.getAplicador().getId() : h.getMaquina().getId();
        String titular = carnet ? h.getAplicador().getNombre() : h.getMaquina().getDescripcion();
        return new HabilitacionResponse(h.getId(), h.getTipo(), titularId, titular, h.getNumero(), h.getVencimiento(),
                h.getEstado(), h.getMotivoRechazo());
    }
}
