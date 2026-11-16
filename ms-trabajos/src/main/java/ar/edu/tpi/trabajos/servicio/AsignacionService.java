package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
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
 * la asignación localmente.
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
        OrdenTrabajo asignada = persistencia.guardar(ordenId, p.getId(), reserva.id());
        log.info("Orden {} ASIGNADA a la postulación {} (reserva {})", ordenId, p.getId(), reserva.id());
        return consultas.detalleDe(asignada);
    }
}
