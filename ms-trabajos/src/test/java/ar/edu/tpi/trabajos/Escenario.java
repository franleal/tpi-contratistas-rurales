package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Respuestas simuladas de ms-contratistas y Open-Meteo para un escenario apto: máquina 3 de c1
 * (25 ha/h, base en el lote), aplicador 4, agenda libre y pronóstico apto toda la semana.
 */
final class Escenario {

    static final LocalDateTime INICIO = RelojFijo.AHORA.plusDays(1).withHour(8); // 11/12 08:00

    private Escenario() {
    }

    static ContratistasClient.Habilitacion habilitada(long maquina, String duenio) {
        return new ContratistasClient.Habilitacion(maquina, 4L, INICIO.toLocalDate(), true, null, "PULVERIZADORA",
                new BigDecimal("25.00"), new BigDecimal("-31.400000"), new BigDecimal("-64.200000"), duenio);
    }

    static void apto(ContratistasClient contratistas, OpenMeteoClient meteo) {
        when(contratistas.habilitacion(anyLong(), anyLong(), any(LocalDate.class)))
                .thenAnswer(i -> habilitada(i.getArgument(0), "c1"));
        when(contratistas.disponibilidad(anyLong(), any(), any()))
                .thenAnswer(i -> new ContratistasClient.Disponibilidad(i.getArgument(0), i.getArgument(1),
                        i.getArgument(2), true, null));
        LocalDateTime d = RelojFijo.AHORA.toLocalDate().atStartOfDay();
        when(meteo.horas(anyDouble(), anyDouble(), any(), any())).thenReturn(Climas.aptas(d, d.plusDays(10)));
    }

    static void maquinaDe(ContratistasClient contratistas, long maquina, String duenio) {
        when(contratistas.habilitacion(eq(maquina), anyLong(), any(LocalDate.class)))
                .thenReturn(habilitada(maquina, duenio));
    }
}
