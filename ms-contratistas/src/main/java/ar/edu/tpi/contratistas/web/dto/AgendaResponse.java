package ar.edu.tpi.contratistas.web.dto;

import java.util.List;

/** Agenda de una máquina: bloques declarados y reservas (activas y liberadas). */
public record AgendaResponse(Long maquinaId, List<BloqueResponse> bloques, List<ReservaResponse> reservas) {
}
