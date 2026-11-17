package ar.edu.tpi.contratistas.web.dto;

import ar.edu.tpi.contratistas.dominio.EstadoHabilitacion;
import ar.edu.tpi.contratistas.dominio.Habilitacion;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** Estado del último documento habilitante cargado (carnet o inspección). */
public record DocumentoResumen(Long id, String numero, LocalDate vencimiento, EstadoHabilitacion estado,
                               String motivoRechazo) {

    /** El documento más reciente, o null si no hay ninguno. */
    public static DocumentoResumen ultimo(List<Habilitacion> docs) {
        return docs.stream().max(Comparator.comparing(Habilitacion::getId))
                .map(h -> new DocumentoResumen(h.getId(), h.getNumero(), h.getVencimiento(), h.getEstado(),
                        h.getMotivoRechazo()))
                .orElse(null);
    }
}
