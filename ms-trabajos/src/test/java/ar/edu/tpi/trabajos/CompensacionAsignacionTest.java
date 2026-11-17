package ar.edu.tpi.trabajos;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Postulacion;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.PostulacionRepository;
import ar.edu.tpi.trabajos.servicio.AsignacionPersistencia;
import ar.edu.tpi.trabajos.servicio.AsignacionService;
import ar.edu.tpi.trabajos.servicio.ConsultaOrdenService;
import ar.edu.tpi.trabajos.servicio.OrdenService;
import ar.edu.tpi.trabajos.web.dto.AsignacionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Compensación de F-15 con Mockito: si el guardado local falla después de reservar, se libera la
 * reserva, con reintentos acotados si la liberación falla; si la reserva falla, no hay nada que compensar.
 */
@ExtendWith(MockitoExtension.class)
class CompensacionAsignacionTest {

    static final LocalDateTime INICIO = LocalDateTime.of(2026, 12, 11, 8, 0);

    @Mock
    OrdenService ordenes;
    @Mock
    PostulacionRepository postulaciones;
    @Mock
    ContratistasClient contratistas;
    @Mock
    AsignacionPersistencia persistencia;
    @Mock
    ConsultaOrdenService consultas;
    @InjectMocks
    AsignacionService service;

    @BeforeEach
    void setUp() {
        OrdenTrabajo o = Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 14), EstadoOrden.ABIERTA);
        o.setId(7L);
        Postulacion p = new Postulacion();
        p.setId(20L);
        p.setOrden(o);
        p.setMaquinaId(3L);
        p.setAplicadorId(4L);
        p.setInicio(INICIO);
        p.setFin(INICIO.plusHours(4));
        p.setOcupadoDesde(INICIO.minusMinutes(30));
        p.setEstado(EstadoPostulacion.VALIDA);
        when(ordenes.propia(7L)).thenReturn(o);
        when(postulaciones.findById(20L)).thenReturn(Optional.of(p));
        when(contratistas.habilitacion(anyLong(), anyLong(), any())).thenReturn(new ContratistasClient.Habilitacion(
                3L, 4L, INICIO.toLocalDate(), true, null, "PULVERIZADORA", BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.ONE, "c1"));
    }

    void reservaOk() {
        when(contratistas.reservar(3L, 7L, INICIO.minusMinutes(30), INICIO.plusHours(4)))
                .thenReturn(new ContratistasClient.Reserva(99L, 3L, 7L, INICIO, INICIO.plusHours(4), "ACTIVA"));
    }

    @Test
    void falloDeBaseDeDatosLiberaLaReservaYResponde503() {
        reservaOk();
        when(persistencia.guardar(7L, 20L, 99L)).thenThrow(new DataAccessResourceFailureException("db caída"));
        assertThatThrownBy(() -> service.asignar(7L, new AsignacionRequest(20L)))
                .isInstanceOf(DependenciaNoDisponibleException.class).hasMessageContaining("liberada");
        verify(contratistas).liberar(99L);
    }

    @Test
    void ordenAsignadaPorOtroMientrasTantoLiberaLaReservaYResponde409() {
        reservaOk();
        when(persistencia.guardar(7L, 20L, 99L)).thenThrow(new ConflictoEstadoException("ya ASIGNADA"));
        assertThatThrownBy(() -> service.asignar(7L, new AsignacionRequest(20L)))
                .isInstanceOf(ConflictoEstadoException.class);
        verify(contratistas).liberar(99L);
    }

    @Test
    void liberacionQueFallaUnaVezSeReintentaYQuedaLiberada() {
        reservaOk();
        when(persistencia.guardar(7L, 20L, 99L)).thenThrow(new DataAccessResourceFailureException("db caída"));
        doThrow(new DependenciaNoDisponibleException("ms-contratistas no está disponible"))
                .doNothing()
                .when(contratistas).liberar(99L);
        assertThatThrownBy(() -> service.asignar(7L, new AsignacionRequest(20L)))
                .isInstanceOf(DependenciaNoDisponibleException.class).hasMessageContaining("fue liberada");
        verify(contratistas, times(2)).liberar(99L);
    }

    @Test
    void dobleFallaReintentaAcotadoYInformaLiberacionPendiente() {
        reservaOk();
        when(persistencia.guardar(7L, 20L, 99L)).thenThrow(new DataAccessResourceFailureException("db caída"));
        doThrow(new DependenciaNoDisponibleException("ms-contratistas no está disponible"))
                .when(contratistas).liberar(99L);
        assertThatThrownBy(() -> service.asignar(7L, new AsignacionRequest(20L)))
                .isInstanceOf(DependenciaNoDisponibleException.class)
                .hasMessageContaining("orden 7")
                .hasMessageContaining("reserva 99 quedó pendiente");
        verify(contratistas, times(3)).liberar(99L);
    }

    @Test
    void conflictoLocalConLiberacionPendienteInformaLaReserva() {
        reservaOk();
        when(persistencia.guardar(7L, 20L, 99L)).thenThrow(new ConflictoEstadoException("ya ASIGNADA"));
        doThrow(new DependenciaNoDisponibleException("ms-contratistas no está disponible"))
                .when(contratistas).liberar(99L);
        assertThatThrownBy(() -> service.asignar(7L, new AsignacionRequest(20L)))
                .isInstanceOf(DependenciaNoDisponibleException.class)
                .hasMessageContaining("ya ASIGNADA")
                .hasMessageContaining("reserva 99 quedó pendiente");
    }

    @Test
    void rechazoDefinitivoDeLaLiberacionNoSeReintenta() {
        reservaOk();
        when(persistencia.guardar(7L, 20L, 99L)).thenThrow(new DataAccessResourceFailureException("db caída"));
        doThrow(new RecursoNoEncontradoException("No existe la reserva 99")).when(contratistas).liberar(99L);
        assertThatThrownBy(() -> service.asignar(7L, new AsignacionRequest(20L)))
                .isInstanceOf(DependenciaNoDisponibleException.class).hasMessageContaining("pendiente");
        verify(contratistas, times(1)).liberar(99L);
    }

    @Test
    void siLaReservaFallaNoHayNadaQueCompensar() {
        when(contratistas.reservar(anyLong(), anyLong(), any(), any()))
                .thenThrow(new ConflictoEstadoException("tomada"));
        assertThatThrownBy(() -> service.asignar(7L, new AsignacionRequest(20L)))
                .isInstanceOf(ConflictoEstadoException.class);
        verify(persistencia, never()).guardar(anyLong(), anyLong(), anyLong());
        verify(contratistas, never()).liberar(anyLong());
    }

    @Test
    void asignacionCorrectaNoLibera() {
        reservaOk();
        when(persistencia.guardar(7L, 20L, 99L)).thenReturn(new OrdenTrabajo());
        service.asignar(7L, new AsignacionRequest(20L));
        verify(contratistas, never()).liberar(anyLong());
    }
}
