package ar.edu.tpi.contratistas;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.contratistas.dominio.Aplicador;
import ar.edu.tpi.contratistas.dominio.Contratista;
import ar.edu.tpi.contratistas.dominio.Habilitacion;
import ar.edu.tpi.contratistas.dominio.Maquina;
import ar.edu.tpi.contratistas.dominio.TipoMaquina;
import ar.edu.tpi.contratistas.servicio.EvaluadorHabilitacion;
import ar.edu.tpi.contratistas.servicio.EvaluadorHabilitacion.Resultado;
import ar.edu.tpi.contratistas.servicio.ValidadorDocumental;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvaluadorHabilitacionTest {

    static final LocalDate TRABAJO = LocalDate.of(2026, 12, 10);

    private final EvaluadorHabilitacion evaluador = new EvaluadorHabilitacion(new ValidadorDocumental());
    private Maquina maquina;
    private Aplicador aplicador;

    @BeforeEach
    void setUp() {
        Contratista c = new Contratista("c1", "Empresa", "30712345679");
        c.setId(1L);
        maquina = new Maquina();
        maquina.setId(10L);
        maquina.setContratista(c);
        maquina.setTipo(TipoMaquina.PULVERIZADORA);
        aplicador = new Aplicador(c, "Juan", "20111222");
        aplicador.setId(20L);
    }

    static Habilitacion validada(Habilitacion h) {
        h.aprobar();
        return h;
    }

    Habilitacion inspeccion(LocalDate vence) {
        return Habilitacion.inspeccion(maquina, "INS-1", vence);
    }

    Habilitacion carnet(LocalDate vence) {
        return Habilitacion.carnet(aplicador, "CAR-1", vence);
    }

    @Test
    void todoValidadoYVigenteEnLaFechaHabilita() {
        Resultado r = evaluador.evaluar(maquina, List.of(validada(inspeccion(TRABAJO.plusMonths(6)))),
                aplicador, List.of(validada(carnet(TRABAJO))), TRABAJO);
        assertThat(r.habilitado()).isTrue();
    }

    @Test
    void carnetQueVenceAntesDelTrabajoNoHabilitaAunqueHoyEsteVigente() {
        Resultado r = evaluador.evaluar(maquina, List.of(validada(inspeccion(TRABAJO.plusYears(1)))),
                aplicador, List.of(validada(carnet(TRABAJO.minusDays(1)))), TRABAJO);
        assertThat(r.habilitado()).isFalse();
        assertThat(r.motivo()).contains("carnet").contains("vence").contains("RN-02");
    }

    @Test
    void inspeccionPendienteNoHabilita() {
        Resultado r = evaluador.evaluar(maquina, List.of(inspeccion(TRABAJO.plusYears(1))),
                aplicador, List.of(validada(carnet(TRABAJO))), TRABAJO);
        assertThat(r.habilitado()).isFalse();
        assertThat(r.motivo()).contains("PENDIENTE").contains("RN-03");
    }

    @Test
    void carnetRechazadoNoHabilita() {
        Habilitacion rechazado = carnet(TRABAJO.plusYears(1));
        rechazado.rechazar("Foto ilegible");
        Resultado r = evaluador.evaluar(maquina, List.of(validada(inspeccion(TRABAJO.plusYears(1)))),
                aplicador, List.of(rechazado), TRABAJO);
        assertThat(r.motivo()).contains("RECHAZADA");
    }

    @Test
    void sinDocumentosNoHabilita() {
        Resultado r = evaluador.evaluar(maquina, List.of(), aplicador, List.of(), TRABAJO);
        assertThat(r.motivo()).contains("no fue cargada");
    }

    @Test
    void maquinaQueNoEsPulverizadoraNoHabilita() {
        maquina.setTipo(TipoMaquina.OTRA);
        Resultado r = evaluador.evaluar(maquina, List.of(validada(inspeccion(TRABAJO))),
                aplicador, List.of(validada(carnet(TRABAJO))), TRABAJO);
        assertThat(r.motivo()).contains("PULVERIZADORA");
    }

    @Test
    void basteConUnDocumentoVigenteAunqueHayaOtroVencido() {
        Resultado r = evaluador.evaluar(maquina,
                List.of(validada(inspeccion(TRABAJO.minusYears(1))), validada(inspeccion(TRABAJO.plusYears(1)))),
                aplicador, List.of(validada(carnet(TRABAJO))), TRABAJO);
        assertThat(r.habilitado()).isTrue();
    }

    @Test
    void habilitacionResueltaNoSePuedeVolverAResolver() {
        Habilitacion h = validada(carnet(TRABAJO));
        assertThatThrownBy(() -> h.rechazar("x")).isInstanceOf(ConflictoEstadoException.class);
        assertThatThrownBy(h::aprobar).isInstanceOf(ConflictoEstadoException.class);
    }
}
