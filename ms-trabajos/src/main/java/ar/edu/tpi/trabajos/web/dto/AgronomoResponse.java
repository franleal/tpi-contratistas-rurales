package ar.edu.tpi.trabajos.web.dto;

import ar.edu.tpi.trabajos.dominio.Agronomo;
import ar.edu.tpi.trabajos.dominio.EstadoMatricula;

import java.time.LocalDate;

public record AgronomoResponse(Long id, String nombre, String matricula, String provincia, LocalDate vencimiento,
                               EstadoMatricula estado, String motivoRechazo) {

    public static AgronomoResponse de(Agronomo a) {
        return new AgronomoResponse(a.getId(), a.getNombre(), a.getMatricula(), a.getProvincia(), a.getVencimiento(),
                a.getEstado(), a.getMotivoRechazo());
    }
}
