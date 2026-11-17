package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.trabajos.dominio.Asignacion;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Postulacion;
import ar.edu.tpi.trabajos.repositorio.AsignacionRepository;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.PostulacionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guardado local de la asignación en una transacción propia (separada de las llamadas HTTP):
 * la orden pasa a ASIGNADA, la postulación elegida a ASIGNADA y las demás a DESCARTADA.
 */
@Component
public class AsignacionPersistencia {

    private final OrdenTrabajoRepository ordenes;
    private final PostulacionRepository postulaciones;
    private final AsignacionRepository asignaciones;

    public AsignacionPersistencia(OrdenTrabajoRepository ordenes, PostulacionRepository postulaciones,
                                  AsignacionRepository asignaciones) {
        this.ordenes = ordenes;
        this.postulaciones = postulaciones;
        this.asignaciones = asignaciones;
    }

    @Transactional
    public OrdenTrabajo guardar(Long ordenId, Long postulacionId, Long reservaId) {
        OrdenTrabajo orden = ordenes.bloquear(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden " + ordenId));
        orden.exigirEstado(EstadoOrden.ABIERTA, "asignar"); // otra asignación pudo ganar mientras tanto
        Postulacion elegida = null;
        for (Postulacion p : postulaciones.findByOrdenId(ordenId)) {
            if (p.getId().equals(postulacionId)) {
                elegida = p;
            } else if (p.getEstado() == EstadoPostulacion.VALIDA) {
                p.setEstado(EstadoPostulacion.DESCARTADA);
            }
        }
        if (elegida == null || elegida.getEstado() != EstadoPostulacion.VALIDA) {
            throw new ConflictoEstadoException("La postulación " + postulacionId + " ya no está VALIDA");
        }
        elegida.setEstado(EstadoPostulacion.ASIGNADA);
        Asignacion a = new Asignacion();
        a.setOrden(orden);
        a.setPostulacion(elegida);
        a.setReservaId(reservaId);
        asignaciones.save(a);
        orden.avanzarA(EstadoOrden.ASIGNADA);
        return orden;
    }
}
