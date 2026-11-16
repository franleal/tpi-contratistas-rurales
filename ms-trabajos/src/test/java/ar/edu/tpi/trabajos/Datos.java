package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.dominio.Agronomo;
import ar.edu.tpi.trabajos.dominio.EstadoMatricula;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Zona;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Entidades de prueba armadas directamente. */
final class Datos {

    private Datos() {
    }

    static OrdenTrabajo orden(String duenio, Zona zona, LocalDate fechaLimite, EstadoOrden estado) {
        OrdenTrabajo o = new OrdenTrabajo();
        o.setProductorSub(duenio);
        o.setLoteId(1L);
        o.setLoteNombre("Lote 1");
        o.setLoteLat(new BigDecimal("-31.400000"));
        o.setLoteLon(new BigDecimal("-64.200000"));
        o.setSuperficieHa(new BigDecimal("100.00"));
        o.setZona(zona);
        o.setProvincia("Córdoba");
        o.setCategorias(List.of("III", "IV"));
        o.setFechaLimite(fechaLimite);
        o.setEstado(estado);
        return o;
    }

    static Agronomo agronomo(String sub, EstadoMatricula estado, LocalDate vencimiento) {
        Agronomo a = new Agronomo();
        a.setSub(sub);
        a.setNombre("Agrónomo " + sub);
        a.setMatricula("MAT-" + sub);
        a.setProvincia("Córdoba");
        a.setVencimiento(vencimiento);
        a.setEstado(estado);
        return a;
    }
}
