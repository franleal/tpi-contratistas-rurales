package ar.edu.tpi.contratistas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static ar.edu.tpi.contratistas.Usuarios.C1;
import static ar.edu.tpi.contratistas.Usuarios.C2;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HabilitacionControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void cargaCarnetEnPendienteYSeVeEnElAplicador() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        long apl = Fixtures.aplicador(mvc, C1, "20111222");
        mvc.perform(post("/api/habilitaciones").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"CARNET_APLICADOR","titularId":%d,"numero":"CBA-123","vencimiento":"2027-03-01"}"""
                                .formatted(apl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.titularId").value(apl));
        mvc.perform(get("/api/aplicadores").with(C1))
                .andExpect(jsonPath("$[0].carnet.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$[0].carnet.vencimiento").value("2027-03-01"));
    }

    @Test
    void cargaInspeccionDeMaquinaPropia() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        long maq = Fixtures.maquina(mvc, C1);
        mvc.perform(post("/api/habilitaciones").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"INSPECCION_MAQUINA","titularId":%d,"numero":"ITV-9","vencimiento":"2027-06-30"}"""
                                .formatted(maq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("INSPECCION_MAQUINA"));
    }

    @Test
    void titularAjenoDevuelve403() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        Fixtures.empresa(mvc, C2, "30798765432");
        long maq = Fixtures.maquina(mvc, C1);
        mvc.perform(post("/api/habilitaciones").with(C2).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"INSPECCION_MAQUINA","titularId":%d,"numero":"X","vencimiento":"2027-06-30"}"""
                                .formatted(maq)))
                .andExpect(status().isForbidden());
    }

    @Test
    void titularInexistenteDevuelve404() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        mvc.perform(post("/api/habilitaciones").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"CARNET_APLICADOR","titularId":999,"numero":"X","vencimiento":"2027-06-30"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void tipoInvalidoDevuelve400() throws Exception {
        mvc.perform(post("/api/habilitaciones").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"LICENCIA","titularId":1,"numero":"X","vencimiento":"2027-06-30"}"""))
                .andExpect(status().isBadRequest());
    }
}
