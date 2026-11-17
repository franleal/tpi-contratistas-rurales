package ar.edu.tpi.contratistas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** La documentación OpenAPI es pública y solo expone las rutas /api. */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void publicaLaDocumentacionSinToken() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("ms-contratistas"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/maquinas']").exists())
                .andExpect(jsonPath("$.paths['/internal/agenda/reservas']").doesNotExist());
    }
}
