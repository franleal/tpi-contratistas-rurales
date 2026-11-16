package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.AccesoRecursoException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.repositorio.AsignacionRepository;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.RecetaRepository;
import ar.edu.tpi.trabajos.web.dto.AsignacionResponse;
import ar.edu.tpi.trabajos.web.dto.OrdenDetalleResponse;
import ar.edu.tpi.trabajos.web.dto.OrdenResponse;
import ar.edu.tpi.trabajos.web.dto.RecetaResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

/**
 * Listado y detalle según el rol del token: el productor ve las propias, el agrónomo las que
 * esperan receta y el contratista las abiertas (y las que tiene asignadas).
 */
@Service
public class ConsultaOrdenService {

    private final OrdenTrabajoRepository ordenes;
    private final RecetaRepository recetas;
    private final OrdenService ordenService;
    private final AsignacionRepository asignaciones;

    public ConsultaOrdenService(OrdenTrabajoRepository ordenes, RecetaRepository recetas, OrdenService ordenService,
                                AsignacionRepository asignaciones) {
        this.ordenes = ordenes;
        this.recetas = recetas;
        this.ordenService = ordenService;
        this.asignaciones = asignaciones;
    }

    @Transactional(readOnly = true)
    public List<OrdenResponse> listar(EstadoOrden estado) {
        Stream<OrdenTrabajo> visibles;
        if (CurrentUser.tieneRol("PRODUCTOR")) {
            visibles = ordenes.findByProductorSubOrderByIdDesc(CurrentUser.sub()).stream();
        } else if (CurrentUser.tieneRol("AGRONOMO")) {
            visibles = ordenes.findByEstadoInOrderByIdDesc(List.of(EstadoOrden.RECETA_PENDIENTE)).stream();
        } else if (CurrentUser.tieneRol("CONTRATISTA")) {
            visibles = visiblesParaContratista(CurrentUser.sub());
        } else {
            throw new AccesoRecursoException("El rol del usuario no permite consultar órdenes");
        }
        return visibles.filter(o -> estado == null || o.getEstado() == estado).map(OrdenResponse::de).toList();
    }

    /** Las abiertas (para postularse) y las que tiene asignadas, en cualquier estado posterior. */
    private Stream<OrdenTrabajo> visiblesParaContratista(String sub) {
        Stream<OrdenTrabajo> asignadas = asignaciones.findByPostulacionContratistaSub(sub).stream()
                .map(a -> a.getOrden());
        return Stream.concat(ordenes.findByEstadoInOrderByIdDesc(List.of(EstadoOrden.ABIERTA)).stream(), asignadas)
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()));
    }

    @Transactional(readOnly = true)
    public OrdenDetalleResponse detalle(Long id) {
        OrdenTrabajo o = CurrentUser.tieneRol("PRODUCTOR") ? ordenService.propia(id) : ordenService.buscar(id);
        return detalleDe(o);
    }

    /** Arma el detalle completo de una orden ya autorizada. */
    @Transactional(readOnly = true)
    public OrdenDetalleResponse detalleDe(OrdenTrabajo o) {
        RecetaResponse receta = recetas.findByOrdenId(o.getId()).map(RecetaResponse::de).orElse(null);
        AsignacionResponse asignacion = asignaciones.findByOrdenId(o.getId()).map(AsignacionResponse::de)
                .orElse(null);
        return new OrdenDetalleResponse(OrdenResponse.de(o), receta, asignacion);
    }
}
