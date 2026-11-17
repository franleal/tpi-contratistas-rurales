package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.RegistroEjecucion;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.RegistroEjecucionRepository;
import ar.edu.tpi.trabajos.web.dto.OrdenDetalleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** F-17 / RN-17: solo el productor dueño confirma, y solo si hay una ejecución registrada. */
@Service
public class ConfirmacionService {

    private static final Logger log = LoggerFactory.getLogger(ConfirmacionService.class);

    private final OrdenService ordenService;
    private final OrdenTrabajoRepository ordenes;
    private final RegistroEjecucionRepository ejecuciones;
    private final ConsultaOrdenService consultas;
    private final Clock reloj;

    public ConfirmacionService(OrdenService ordenService, OrdenTrabajoRepository ordenes,
                               RegistroEjecucionRepository ejecuciones, ConsultaOrdenService consultas, Clock reloj) {
        this.ordenService = ordenService;
        this.ordenes = ordenes;
        this.ejecuciones = ejecuciones;
        this.consultas = consultas;
        this.reloj = reloj;
    }

    @Transactional
    public OrdenDetalleResponse confirmar(Long ordenId) {
        ordenService.propia(ordenId); // 404 o 403 (RN-17)
        OrdenTrabajo orden = ordenes.bloquear(ordenId).orElseThrow();
        RegistroEjecucion ejecucion = ejecuciones.findByOrdenId(ordenId)
                .orElseThrow(() -> new ConflictoEstadoException("La orden " + ordenId
                        + " no tiene una ejecución registrada para confirmar (RN-17)"));
        orden.exigirEstado(EstadoOrden.EJECUTADA, "confirmar");
        ejecucion.setConfirmadaEn(LocalDateTime.now(reloj));
        orden.avanzarA(EstadoOrden.COMPLETADA);
        log.info("Orden {} COMPLETADA", ordenId);
        return consultas.detalleDe(orden);
    }
}
