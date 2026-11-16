package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import ar.edu.tpi.commons.excepcion.NegocioException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Postulacion;
import ar.edu.tpi.trabajos.repositorio.PostulacionRepository;
import ar.edu.tpi.trabajos.web.dto.AsignacionRequest;
import ar.edu.tpi.trabajos.web.dto.OrdenDetalleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * F-15: revalida habilitaciones (RN-04), reserva la agenda en ms-contratistas (RN-13) y guarda
 * la asignación localmente. Si el guardado local falla, libera la reserva (compensación, D-07),
 * con reintentos acotados; si no logra liberarla responde 503 indicando que quedó pendiente.
 */
@Service
public class AsignacionService {

    private static final Logger log = LoggerFactory.getLogger(AsignacionService.class);

    private final OrdenService ordenes;
    private final PostulacionRepository postulaciones;
    private final ContratistasClient contratistas;
    private final AsignacionPersistencia persistencia;
    private final ConsultaOrdenService consultas;

    public AsignacionService(OrdenService ordenes, PostulacionRepository postulaciones,
                             ContratistasClient contratistas, AsignacionPersistencia persistencia,
                             ConsultaOrdenService consultas) {
        this.ordenes = ordenes;
        this.postulaciones = postulaciones;
        this.contratistas = contratistas;
        this.persistencia = persistencia;
        this.consultas = consultas;
    }

    /** Intentos de liberación (el primero más los reintentos); el DELETE es idempotente. */
    static final int INTENTOS_LIBERACION = 3;
    /** Pausa base entre intentos; crece linealmente (200 ms, 400 ms). */
    static final long PAUSA_BASE_MS = 200;

    /**
     * Compensación: si el guardado local falla después de reservar, se libera la reserva para no
     * dejar la máquina ocupada por una orden que no quedó asignada. Como el DELETE de la reserva es
     * idempotente, una falla transitoria (503) se reintenta hasta {@link #INTENTOS_LIBERACION} veces.
     * Si aun así no se libera, se registra el id de la reserva para resolverlo a mano.
     *
     * @return true si la reserva quedó liberada; false si la liberación quedó pendiente
     */
    boolean compensar(Long ordenId, Long reservaId, RuntimeException causa) {
        log.warn("Falló el guardado de la asignación de la orden {}: se libera la reserva {} ({})", ordenId,
                reservaId, causa.getMessage());
        for (int intento = 1; intento <= INTENTOS_LIBERACION; intento++) {
            try {
                contratistas.liberar(reservaId);
                if (intento > 1) {
                    log.info("Reserva {} de la orden {} liberada en el intento {}", reservaId, ordenId, intento);
                }
                return true;
            } catch (DependenciaNoDisponibleException e) {
                log.warn("Intento {}/{} de liberar la reserva {} falló: {}", intento, INTENTOS_LIBERACION, reservaId,
                        e.getMessage());
                if (intento < INTENTOS_LIBERACION && !pausar(intento)) {
                    break;
                }
            } catch (RuntimeException e) {
                // 404/409/422: respuesta definitiva, reintentar no cambia el resultado
                log.warn("La liberación de la reserva {} fue rechazada: {}", reservaId, e.getMessage());
                break;
            }
        }
        log.error("LIBERACION_PENDIENTE reservaId={} ordenId={}: no se pudo liberar la reserva; requiere "
                + "intervención manual (DELETE /internal/agenda/reservas/{})", reservaId, ordenId, reservaId);
        return false;
    }

    private static boolean pausar(int intento) {
        try {
            Thread.sleep(PAUSA_BASE_MS * intento);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public OrdenDetalleResponse asignar(Long ordenId, AsignacionRequest req) {
        OrdenTrabajo orden = ordenes.propia(ordenId);
        orden.exigirEstado(EstadoOrden.ABIERTA, "asignar");
        Postulacion p = postulaciones.findById(req.postulacionId())
                .filter(x -> x.getOrden().getId().equals(ordenId))
                .orElseThrow(() -> new RecursoNoEncontradoException("La orden " + ordenId
                        + " no tiene la postulación " + req.postulacionId()));
        if (p.getEstado() != EstadoPostulacion.VALIDA) {
            throw new ConflictoEstadoException("La postulación " + p.getId() + " está " + p.getEstado());
        }

        // RN-04: la vigencia se recalcula al asignar (pudo vencer o ser rechazada desde la postulación)
        PostulacionService.exigirHabilitado(
                contratistas.habilitacion(p.getMaquinaId(), p.getAplicadorId(), p.getInicio().toLocalDate()));
        if (!p.getFin().toLocalDate().equals(p.getInicio().toLocalDate())) {
            PostulacionService.exigirHabilitado(
                    contratistas.habilitacion(p.getMaquinaId(), p.getAplicadorId(), p.getFin().toLocalDate()));
        }

        // RN-13: 409 si otra asignación tomó el intervalo mientras tanto
        ContratistasClient.Reserva reserva = contratistas.reservar(p.getMaquinaId(), ordenId, p.getOcupadoDesde(),
                p.getFin());
        OrdenTrabajo asignada;
        try {
            asignada = persistencia.guardar(ordenId, p.getId(), reserva.id());
        } catch (RuntimeException e) {
            if (!compensar(ordenId, reserva.id(), e)) {
                throw new DependenciaNoDisponibleException("No se pudo guardar la asignación de la orden " + ordenId
                        + " (" + e.getMessage() + ") y la liberación de la reserva " + reserva.id()
                        + " quedó pendiente", e);
            }
            if (e instanceof NegocioException negocio) {
                throw negocio;
            }
            throw new DependenciaNoDisponibleException("No se pudo guardar la asignación de la orden " + ordenId
                    + "; la reserva " + reserva.id() + " fue liberada", e);
        }
        log.info("Orden {} ASIGNADA a la postulación {} (reserva {})", ordenId, p.getId(), reserva.id());
        return consultas.detalleDe(asignada);
    }
}
