package ar.edu.tpi.campos.cliente;

import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

/**
 * Cliente de Georef Argentina (https://apis.datos.gob.ar/georef). Cualquier falla de red,
 * timeout, error HTTP o respuesta sin el formato esperado se traduce en 503: el lote no se
 * guarda sin clasificar (RN-05, falla segura).
 */
@Component
public class GeorefClient {

    private static final Logger log = LoggerFactory.getLogger(GeorefClient.class);

    /** Un departamento tiene a lo sumo unas pocas centenas de localidades. */
    static final int MAX_LOCALIDADES = 5000;

    private final RestClient http;

    public GeorefClient(@Qualifier("georefRestClient") RestClient http) {
        this.http = http;
    }

    /** Provincia y departamento del punto según Georef. */
    public record Ubicacion(String provinciaId, String provincia, String departamentoId, String departamento) {
    }

    /**
     * Ubica un punto. Devuelve vacío si Georef no lo encuentra dentro de Argentina
     * (provincia nula), lo que el servicio traduce en 422.
     */
    public Optional<Ubicacion> ubicar(double lat, double lon) {
        UbicacionResponse resp = llamar(() -> http.get()
                .uri(u -> u.path("/ubicacion").queryParam("lat", lat).queryParam("lon", lon).build())
                .retrieve()
                .body(UbicacionResponse.class), "ubicacion");
        if (resp == null || resp.ubicacion() == null) {
            throw new DependenciaNoDisponibleException("Georef devolvió una respuesta inesperada al ubicar el lote");
        }
        Unidad provincia = resp.ubicacion().provincia();
        if (provincia == null || provincia.nombre() == null) {
            return Optional.empty();
        }
        Unidad depto = resp.ubicacion().departamento();
        return Optional.of(new Ubicacion(provincia.id(), provincia.nombre(),
                depto == null ? null : depto.id(), depto == null ? null : depto.nombre()));
    }

    /** Localidad con su centroide, para medir distancias (RN-05, RN-08). */
    public record Localidad(String nombre, double lat, double lon) {
    }

    /**
     * Localidades del departamento (con su centroide). Se filtra por provincia y
     * departamento porque los ids de departamento solo son únicos dentro de la provincia.
     */
    public List<Localidad> localidades(String provinciaId, String departamentoId) {
        LocalidadesResponse resp = llamar(() -> http.get()
                .uri(u -> u.path("/localidades")
                        .queryParam("provincia", provinciaId)
                        .queryParam("departamento", departamentoId)
                        .queryParam("campos", "id,nombre,centroide")
                        .queryParam("max", MAX_LOCALIDADES)
                        .build())
                .retrieve()
                .body(LocalidadesResponse.class), "localidades");
        if (resp == null || resp.localidades() == null) {
            throw new DependenciaNoDisponibleException("Georef devolvió una respuesta inesperada al listar localidades");
        }
        return resp.localidades().stream()
                .filter(l -> l.centroide() != null && l.centroide().lat() != null && l.centroide().lon() != null)
                .map(l -> new Localidad(l.nombre(), l.centroide().lat(), l.centroide().lon()))
                .toList();
    }

    <T> T llamar(java.util.function.Supplier<T> llamada, String recurso) {
        try {
            return llamada.get();
        } catch (RestClientException e) {
            log.warn("Georef /{} no disponible: {}", recurso, e.getMessage());
            throw new DependenciaNoDisponibleException("Georef no está disponible: no se puede clasificar el lote", e);
        }
    }

    // ---- DTOs del contrato de Georef (propios de este cliente) ----

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UbicacionResponse(UbicacionDto ubicacion) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UbicacionDto(Unidad provincia, Unidad departamento) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Unidad(String id, String nombre) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LocalidadesResponse(List<LocalidadDto> localidades) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LocalidadDto(String id, String nombre, Centroide centroide) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Centroide(Double lat, Double lon) {
    }
}
