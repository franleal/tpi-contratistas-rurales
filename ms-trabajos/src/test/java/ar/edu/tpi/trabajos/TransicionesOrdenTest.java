package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.trabajos.dominio.Agronomo;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** RN-18: la orden solo avanza un paso por vez y en orden. */
class TransicionesOrdenTest {

    @Test
    void recorreTodoElCicloEnOrden() {
        OrdenTrabajo o = new OrdenTrabajo();
        for (EstadoOrden e : List.of(EstadoOrden.ABIERTA, EstadoOrden.ASIGNADA, EstadoOrden.EJECUTADA,
                EstadoOrden.COMPLETADA)) {
            o.avanzarA(e);
        }
        assertThat(o.getEstado()).isEqualTo(EstadoOrden.COMPLETADA);
        assertThat(EstadoOrden.COMPLETADA.siguiente()).isNull();
    }

    @ParameterizedTest(name = "{0} → {1} es inválida")
    @CsvSource({
            "RECETA_PENDIENTE, ASIGNADA",
            "RECETA_PENDIENTE, COMPLETADA",
            "ABIERTA, EJECUTADA",
            "ABIERTA, ABIERTA",
            "ASIGNADA, ABIERTA",
            "EJECUTADA, ASIGNADA",
            "COMPLETADA, RECETA_PENDIENTE"})
    void saltosYRetrocesosDevuelven409(EstadoOrden desde, EstadoOrden hacia) {
        OrdenTrabajo o = new OrdenTrabajo();
        o.setEstado(desde);
        assertThatThrownBy(() -> o.avanzarA(hacia)).isInstanceOf(ConflictoEstadoException.class)
                .hasMessageContaining("RN-18");
        assertThat(o.getEstado()).isEqualTo(desde);
    }

    @Test
    void exigirEstadoInformaElEstadoActual() {
        OrdenTrabajo o = new OrdenTrabajo();
        assertThatThrownBy(() -> o.exigirEstado(EstadoOrden.ABIERTA, "postularse"))
                .isInstanceOf(ConflictoEstadoException.class)
                .hasMessageContaining("RECETA_PENDIENTE");
    }

    @Test
    void categoriasSeGuardanComoTexto() {
        OrdenTrabajo o = new OrdenTrabajo();
        o.setCategorias(List.of("III", "IV"));
        assertThat(o.getCategoriasPermitidas()).isEqualTo("III,IV");
        assertThat(o.getCategorias()).containsExactly("III", "IV");
    }

    @Test
    void matriculaResueltaNoSeVuelveAResolver() {
        Agronomo a = new Agronomo();
        a.aprobar();
        assertThatThrownBy(() -> a.rechazar("x")).isInstanceOf(ConflictoEstadoException.class);
    }
}
