package ar.edu.tpi.contratistas;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Atajos para preparar datos por la API en las pruebas. */
final class Fixtures {

    private Fixtures() {
    }

    static long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    static String crear(MockMvc mvc, RequestPostProcessor quien, String url, String body) throws Exception {
        return mvc.perform(post(url).with(quien).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
    }

    static void empresa(MockMvc mvc, RequestPostProcessor quien, String cuit) throws Exception {
        crear(mvc, quien, "/api/contratistas", """
                {"razonSocial":"Empresa %s","cuit":"%s"}""".formatted(cuit, cuit));
    }

    static long aplicador(MockMvc mvc, RequestPostProcessor quien, String documento) throws Exception {
        return id(crear(mvc, quien, "/api/aplicadores", """
                {"nombre":"Aplicador %s","documento":"%s"}""".formatted(documento, documento)));
    }

    static long habilitacion(MockMvc mvc, RequestPostProcessor quien, String tipo, long titularId, String vence)
            throws Exception {
        return id(crear(mvc, quien, "/api/habilitaciones", """
                {"tipo":"%s","titularId":%d,"numero":"N-%d","vencimiento":"%s"}""".formatted(tipo, titularId, titularId, vence)));
    }

    static void aprobar(MockMvc mvc, long habilitacionId) throws Exception {
        mvc.perform(post("/api/admin/habilitaciones/" + habilitacionId + "/aprobar").with(Usuarios.ADMIN));
    }

    static long maquina(MockMvc mvc, RequestPostProcessor quien) throws Exception {
        return id(crear(mvc, quien, "/api/maquinas", """
                {"descripcion":"Pulverizadora autopropulsada","anchoM":28,"rendimientoHaH":25,"baseLat":-31.4,"baseLon":-64.18}"""));
    }
}
