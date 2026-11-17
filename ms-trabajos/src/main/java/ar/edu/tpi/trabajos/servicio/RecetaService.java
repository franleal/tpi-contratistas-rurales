package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.commons.excepcion.AccesoRecursoException;
import ar.edu.tpi.commons.excepcion.DatosInvalidosException;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import ar.edu.tpi.commons.util.Vigencia;
import ar.edu.tpi.trabajos.dominio.Agronomo;
import ar.edu.tpi.trabajos.dominio.CategoriaToxicologica;
import ar.edu.tpi.trabajos.dominio.EstadoMatricula;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Receta;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.AgronomoRepository;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.RecetaRepository;
import ar.edu.tpi.trabajos.web.dto.OrdenDetalleResponse;
import ar.edu.tpi.trabajos.web.dto.OrdenResponse;
import ar.edu.tpi.trabajos.web.dto.RecetaRequest;
import ar.edu.tpi.trabajos.web.dto.RecetaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F-11: firma de la receta. Usa solo la copia del lote guardada en la orden. */
@Service
public class RecetaService {

    private static final Logger log = LoggerFactory.getLogger(RecetaService.class);

    private final OrdenTrabajoRepository ordenes;
    private final AgronomoRepository agronomos;
    private final RecetaRepository recetas;

    public RecetaService(OrdenTrabajoRepository ordenes, AgronomoRepository agronomos, RecetaRepository recetas) {
        this.ordenes = ordenes;
        this.agronomos = agronomos;
        this.recetas = recetas;
    }

    @Transactional
    public OrdenDetalleResponse firmar(Long ordenId, RecetaRequest req) {
        CategoriaToxicologica categoria = CategoriaToxicologica.desdeCodigo(req.categoriaToxicologica())
                .orElseThrow(() -> new DatosInvalidosException("Categoría toxicológica inexistente: "
                        + req.categoriaToxicologica() + ". Válidas: Ia, Ib, II, III, IV"));
        OrdenTrabajo orden = ordenes.bloquear(ordenId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden " + ordenId));
        orden.exigirEstado(EstadoOrden.RECETA_PENDIENTE, "firmar la receta");

        // RN-01: matrícula validada y vigente hasta la fecha límite de la orden
        Agronomo agronomo = agronomos.findBySub(CurrentUser.sub())
                .orElseThrow(() -> new AccesoRecursoException("El agrónomo no tiene matrícula registrada (RN-01)"));
        if (agronomo.getEstado() != EstadoMatricula.VALIDADA) {
            throw new AccesoRecursoException("La matrícula " + agronomo.getMatricula() + " está "
                    + agronomo.getEstado() + ": solo una matrícula VALIDADA puede firmar recetas (RN-01)");
        }
        if (!Vigencia.cubre(agronomo.getVencimiento(), orden.getFechaLimite())) {
            throw new ReglaNegocioException("La matrícula vence el " + agronomo.getVencimiento()
                    + ", antes de la fecha límite de la orden " + orden.getFechaLimite() + " (RN-01)");
        }
        // RN-07: en amortiguamiento, solo categorías permitidas según la copia tomada al crear la orden
        if (orden.getZona() == Zona.AMORTIGUAMIENTO
                && orden.getCategorias().stream().noneMatch(c -> c.equalsIgnoreCase(categoria.codigo()))) {
            throw new ReglaNegocioException("La categoría " + categoria.codigo() + " no está permitida en zona de "
                    + "amortiguamiento de " + orden.getProvincia() + ". Permitidas: " + orden.getCategorias()
                    + " (RN-07)");
        }

        Receta r = new Receta();
        r.setOrden(orden);
        r.setAgronomo(agronomo);
        r.setProducto(req.producto().trim());
        r.setCategoriaToxicologica(categoria.codigo());
        r.setDosis(req.dosis().trim());
        recetas.save(r);
        orden.avanzarA(EstadoOrden.ABIERTA);
        log.info("Receta firmada en la orden {} por la matrícula {}: orden ABIERTA", ordenId, agronomo.getMatricula());
        return new OrdenDetalleResponse(OrdenResponse.de(orden), RecetaResponse.de(r));
    }
}
