package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.AccesoRecursoException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.RecetaRepository;
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

    public ConsultaOrdenService(OrdenTrabajoRepository ordenes, RecetaRepository recetas, OrdenService ordenService) {
        this.ordenes = ordenes;
        this.recetas = recetas;
        this.ordenService = ordenService;
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

    /** Punto de extensión: las asignadas al contratista se suman cuando existe la asignación. */
    protected Stream<OrdenTrabajo> visiblesParaContratista(String sub) {
        return ordenes.findByEstadoInOrderByIdDesc(List.of(EstadoOrden.ABIERTA)).stream();
    }

    @Transactional(readOnly = true)
    public OrdenDetalleResponse detalle(Long id) {
        OrdenTrabajo o = CurrentUser.tieneRol("PRODUCTOR") ? ordenService.propia(id) : ordenService.buscar(id);
        RecetaResponse receta = recetas.findByOrdenId(id).map(RecetaResponse::de).orElse(null);
        return new OrdenDetalleResponse(OrdenResponse.de(o), receta);
    }
}
