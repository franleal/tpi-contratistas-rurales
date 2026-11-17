package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.cliente.CamposClient;
import ar.edu.tpi.trabajos.dominio.Zona;

import java.math.BigDecimal;
import java.util.List;

/** Lotes de prueba como los devolvería ms-campos. */
final class Lotes {

    private Lotes() {
    }

    static CamposClient.Lote lote(long id, String duenio, Zona zona) {
        return new CamposClient.Lote(id, duenio, "Lote " + id, new BigDecimal("-31.400000"),
                new BigDecimal("-64.200000"), new BigDecimal("100.00"), "Córdoba", "Capital", zona,
                List.of("III", "IV"), zona != Zona.EXCLUSION);
    }
}
