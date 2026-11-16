package ar.edu.tpi.campos.servicio;

import ar.edu.tpi.campos.cliente.GeorefClient;
import ar.edu.tpi.campos.cliente.GeorefClient.Localidad;
import ar.edu.tpi.campos.cliente.GeorefClient.Ubicacion;
import ar.edu.tpi.campos.dominio.Establecimiento;
import ar.edu.tpi.campos.dominio.Lote;
import ar.edu.tpi.campos.dominio.ParametroZona;
import ar.edu.tpi.campos.repositorio.LoteRepository;
import ar.edu.tpi.campos.servicio.ClasificadorZona.Clasificacion;
import ar.edu.tpi.campos.web.dto.LoteRequest;
import ar.edu.tpi.campos.web.dto.LoteResponse;
import ar.edu.tpi.commons.excepcion.ReglaNegocioException;
import ar.edu.tpi.commons.util.GeoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * F-02: registro de lotes con clasificación de zona (RN-05).
 * <p>
 * Las llamadas a Georef se hacen antes de abrir la transacción de guardado: si Georef falla
 * se responde 503 y no se guarda nada (nunca un lote sin clasificar).
 */
@Service
public class LoteService {

    private static final Logger log = LoggerFactory.getLogger(LoteService.class);

    private final LoteRepository lotes;
    private final EstablecimientoService establecimientos;
    private final ParametroZonaService parametros;
    private final GeorefClient georef;
    private final ClasificadorZona clasificador;

    public LoteService(LoteRepository lotes, EstablecimientoService establecimientos, ParametroZonaService parametros,
                       GeorefClient georef, ClasificadorZona clasificador) {
        this.lotes = lotes;
        this.establecimientos = establecimientos;
        this.parametros = parametros;
        this.georef = georef;
        this.clasificador = clasificador;
    }

    public LoteResponse registrar(LoteRequest req) {
        Establecimiento establecimiento = establecimientos.propio(req.establecimientoId());

        double lat = GeoUtils.redondearCoordenada(req.lat().doubleValue());
        double lon = GeoUtils.redondearCoordenada(req.lon().doubleValue());

        // RN-05: ubicación del punto. Fuera de Argentina -> 422; Georef caído -> 503
        Ubicacion ubicacion = georef.ubicar(lat, lon)
                .orElseThrow(() -> new ReglaNegocioException(
                        "Las coordenadas (" + lat + ", " + lon + ") están fuera de Argentina (RN-05)"));
        if (ubicacion.departamentoId() == null) {
            throw new ReglaNegocioException("Georef no pudo determinar el departamento del punto (RN-05)");
        }
        ParametroZona param = parametros.deProvincia(ubicacion.provincia());

        List<Localidad> localidades = georef.localidades(ubicacion.provinciaId(), ubicacion.departamentoId());
        Clasificacion c = clasificador.clasificar(lat, lon, localidades,
                param.getDistanciaExclusionM(), param.getDistanciaAmortiguamientoM());

        Lote lote = new Lote();
        lote.setEstablecimiento(establecimiento);
        lote.setNombre(req.nombre().trim());
        lote.setLat(BigDecimal.valueOf(lat).setScale(6, RoundingMode.HALF_UP));
        lote.setLon(BigDecimal.valueOf(lon).setScale(6, RoundingMode.HALF_UP));
        lote.setSuperficieHa(req.superficieHa().setScale(2, RoundingMode.HALF_UP));
        lote.setCultivo(req.cultivo().trim());
        lote.setProvincia(ubicacion.provincia());
        lote.setDepartamento(ubicacion.departamento());
        lote.setZona(c.zona());
        lote.setLocalidadCercana(c.localidadCercana());
        lote.setDistanciaLocalidadM(c.distanciaM());
        Lote guardado = lotes.save(lote);

        log.info("Lote {} clasificado en {} ({} m de {}, {})", guardado.getId(), c.zona(), c.distanciaM(),
                c.localidadCercana(), ubicacion.provincia());
        return LoteResponse.de(guardado);
    }
}
