package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.trabajos.cliente.CamposClient;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.web.dto.OrdenRequest;
import ar.edu.tpi.trabajos.web.dto.OrdenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-10 y consultas de órdenes. */
@Service
public class OrdenService {

    private static final Logger log = LoggerFactory.getLogger(OrdenService.class);

    private final OrdenTrabajoRepository ordenes;
    private final CamposClient campos;

    public OrdenService(OrdenTrabajoRepository ordenes, CamposClient campos) {
        this.ordenes = ordenes;
        this.campos = campos;
    }

    /**
     * Crea la orden en RECETA_PENDIENTE copiando los datos del lote. La llamada a ms-campos se
     * hace fuera de la transacción: si falla (503) no se guarda nada.
     */
    public OrdenResponse crear(OrdenRequest req) {
        CamposClient.Lote lote = campos.lote(req.loteId());
        CurrentUser.verificarDuenio(lote.productorSub(), "el lote " + req.loteId());
        if (!lote.admiteOrdenes()) {
            throw new ReglaNegocioException("El lote " + lote.id() + " está en zona de " + lote.zona()
                    + " y no admite órdenes de pulverización (RN-06)");
        }
        OrdenTrabajo o = new OrdenTrabajo();
        o.setProductorSub(lote.productorSub());
        o.setLoteId(lote.id());
        o.setLoteNombre(lote.nombre());
        o.setLoteLat(lote.lat());
        o.setLoteLon(lote.lon());
        o.setSuperficieHa(lote.superficieHa());
        o.setZona(lote.zona());
        o.setProvincia(lote.provincia());
        o.setCategorias(lote.categoriasPermitidas());
        o.setFechaLimite(req.fechaLimite());
        OrdenTrabajo guardada = ordenes.save(o);
        log.info("Orden {} creada sobre el lote {} ({})", guardada.getId(), lote.id(), lote.zona());
        return OrdenResponse.de(guardada);
    }

    @Transactional(readOnly = true)
    public OrdenTrabajo buscar(Long id) {
        return ordenes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden " + id));
    }

    /** Orden del productor del token: 404 si no existe, 403 si es ajena. */
    @Transactional(readOnly = true)
    public OrdenTrabajo propia(Long id) {
        OrdenTrabajo o = buscar(id);
        CurrentUser.verificarDuenio(o.getProductorSub(), "la orden " + id);
        return o;
    }
}
