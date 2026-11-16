package ar.edu.tpi.campos;

import ar.edu.tpi.campos.cliente.GeorefClient;
import ar.edu.tpi.campos.cliente.GeorefClient.Localidad;
import ar.edu.tpi.campos.cliente.GeorefClient.Ubicacion;
import ar.edu.tpi.campos.dominio.ParametroZona;
import ar.edu.tpi.campos.repositorio.LoteRepository;
import ar.edu.tpi.campos.repositorio.ParametroZonaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoteControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ParametroZonaRepository parametros;
    @Autowired
    LoteRepository lotes;
    @MockitoBean
    GeorefClient georef;

    static final RequestPostProcessor P1 = jwt().jwt(j -> j.subject("p1").claim("preferred_username", "p1"));
    static final RequestPostProcessor P2 = jwt().jwt(j -> j.subject("p2").claim("preferred_username", "p2"));

    @BeforeEach
    void setUp() {
        parametros.save(new ParametroZona("Córdoba", 1000, 3000, List.of("III", "IV")));
        when(georef.ubicar(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(new Ubicacion("14", "Córdoba", "14119", "Río Segundo")));
        when(georef.localidades(anyString(), anyString()))
                .thenReturn(List.of(new Localidad("Pilar", -31.0, -64.0)));
    }

    long crearEstablecimiento(RequestPostProcessor quien) throws Exception {
        String body = mvc.perform(post("/api/establecimientos").with(quien).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Campo","provincia":"Córdoba"}"""))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    String lote(long establecimientoId, double lat) {
        return """
                {"establecimientoId":%d,"nombre":"Lote Norte","lat":%s,"lon":-64.0,"superficieHa":120.5,"cultivo":"Soja"}"""
                .formatted(establecimientoId, lat);
    }

    @Test
    void registraLoteLibreConDatosDeGeoref() throws Exception {
        long est = crearEstablecimiento(P1);
        mvc.perform(post("/api/lotes").with(P1).contentType(MediaType.APPLICATION_JSON).content(lote(est, -31.1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zona").value("LIBRE"))
                .andExpect(jsonPath("$.provincia").value("Córdoba"))
                .andExpect(jsonPath("$.departamento").value("Río Segundo"))
                .andExpect(jsonPath("$.localidadCercana").value("Pilar"))
                .andExpect(jsonPath("$.distanciaLocalidadM").isNumber());
    }

    @Test
    void registraLoteEnExclusion() throws Exception {
        long est = crearEstablecimiento(P1);
        mvc.perform(post("/api/lotes").with(P1).contentType(MediaType.APPLICATION_JSON).content(lote(est, -31.005)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.zona").value("EXCLUSION"));
    }

    @Test
    void establecimientoAjenoDevuelve403() throws Exception {
        long est = crearEstablecimiento(P1);
        mvc.perform(post("/api/lotes").with(P2).contentType(MediaType.APPLICATION_JSON).content(lote(est, -31.1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void fueraDeArgentinaDevuelve422() throws Exception {
        long est = crearEstablecimiento(P1);
        when(georef.ubicar(anyDouble(), anyDouble())).thenReturn(Optional.empty());
        mvc.perform(post("/api/lotes").with(P1).contentType(MediaType.APPLICATION_JSON).content(lote(est, -20)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(lotes.count()).isZero();
    }

    @Test
    void provinciaSinParametrosDevuelve422() throws Exception {
        long est = crearEstablecimiento(P1);
        when(georef.ubicar(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(new Ubicacion("74", "San Juan", "74001", "Capital")));
        mvc.perform(post("/api/lotes").with(P1).contentType(MediaType.APPLICATION_JSON).content(lote(est, -31.5)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("San Juan")));
    }

    long crearLote(RequestPostProcessor quien) throws Exception {
        long est = crearEstablecimiento(quien);
        String body = mvc.perform(post("/api/lotes").with(quien).contentType(MediaType.APPLICATION_JSON)
                .content(lote(est, -31.1))).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void consultaLotePropio() throws Exception {
        long id = crearLote(P1);
        mvc.perform(get("/api/lotes/" + id).with(P1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.zona").value("LIBRE"));
    }

    @Test
    void loteAjenoDevuelve403() throws Exception {
        long id = crearLote(P1);
        mvc.perform(get("/api/lotes/" + id).with(P2)).andExpect(status().isForbidden());
    }

    @Test
    void contratoInternoIncluyeDuenioYCategorias() throws Exception {
        long id = crearLote(P1);
        // Lo invoca ms-trabajos con el token de cualquier usuario: no se aplica propiedad
        mvc.perform(get("/internal/lotes/" + id).with(P2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productorSub").value("p1"))
                .andExpect(jsonPath("$.superficieHa").value(120.5))
                .andExpect(jsonPath("$.zona").value("LIBRE"))
                .andExpect(jsonPath("$.categoriasPermitidas[0]").value("III"));
    }

    @Test
    void contratoInternoSinTokenDevuelve401() throws Exception {
        mvc.perform(get("/internal/lotes/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void loteInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/lotes/999").with(P1)).andExpect(status().isNotFound());
    }

    @Test
    void superficieNegativaDevuelve400() throws Exception {
        long est = crearEstablecimiento(P1);
        mvc.perform(post("/api/lotes").with(P1).contentType(MediaType.APPLICATION_JSON)
                        .content(lote(est, -31.1).replace("120.5", "-3")))
                .andExpect(status().isBadRequest());
    }
}
