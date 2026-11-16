package ar.edu.tpi.trabajos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadServicioTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void sinTokenDevuelve401ConFormatoComun() throws Exception {
        mvc.perform(get("/api/ordenes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void rutaInternaTambienExigeToken() throws Exception {
        mvc.perform(get("/internal/no-existe")).andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenValidoPasaElFiltro() throws Exception {
        mvc.perform(get("/api/no-existe").with(jwt().jwt(j -> j.subject("sub-x"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    @Test
    void healthEsPublico() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
