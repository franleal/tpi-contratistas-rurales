package ar.edu.tpi.campos;

import ar.edu.tpi.campos.cliente.GeorefClient;
import ar.edu.tpi.campos.cliente.GeorefClient.Ubicacion;
import ar.edu.tpi.campos.dominio.ParametroZona;
import ar.edu.tpi.campos.repositorio.LoteRepository;
import ar.edu.tpi.campos.repositorio.ParametroZonaRepository;
import ar.edu.tpi.commons.excepcion.DependenciaNoDisponibleException;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tarjeta 02-14: Georef responde la ubicación pero falla al listar localidades: tampoco se guarda. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class FalloParcialGeorefTest {

    static final RequestPostProcessor P1 = jwt().jwt(j -> j.subject("p1").claim("preferred_username", "p1"));

    @Autowired
    MockMvc mvc;
    @Autowired
    LoteRepository lotes;
    @Autowired
    ParametroZonaRepository parametros;
    @MockitoBean
    GeorefClient georef;

    @Test
    void falloEnLocalidadesDevuelve503SinGuardar() throws Exception {
        parametros.save(new ParametroZona("Córdoba", 1000, 3000, List.of("III", "IV")));
        when(georef.ubicar(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(new Ubicacion("14", "Córdoba", "14119", "Río Segundo")));
        when(georef.localidades(anyString(), anyString()))
                .thenThrow(new DependenciaNoDisponibleException("Georef no está disponible"));

        String body = mvc.perform(post("/api/establecimientos").with(P1).contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nombre":"Campo","provincia":"Córdoba"}""")).andReturn().getResponse().getContentAsString();
        long est = ((Number) JsonPath.read(body, "$.id")).longValue();

        mvc.perform(post("/api/lotes").with(P1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"establecimientoId":%d,"nombre":"L","lat":-31.6,"lon":-63.9,"superficieHa":50,"cultivo":"Maíz"}"""
                                .formatted(est)))
                .andExpect(status().isServiceUnavailable());

        assertThat(lotes.count()).isZero();
    }
}
