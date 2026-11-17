package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.EstadoReserva;
import ar.edu.tpi.contratistas.dominio.Reserva;

import java.time.LocalDateTime;

public record ReservaResponse(Long id, Long maquinaId, Long ordenId, LocalDateTime desde, LocalDateTime hasta,
                              EstadoReserva estado) {

    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(r.getId(), r.getMaquina().getId(), r.getOrdenId(), r.getDesde(), r.getHasta(),
                r.getEstado());
    }
}
