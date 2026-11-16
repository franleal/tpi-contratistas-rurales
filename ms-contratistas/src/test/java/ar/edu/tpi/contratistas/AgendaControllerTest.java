package ar.edu.tpi.contratistas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static ar.edu.tpi.contratistas.Usuarios.C1;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AgendaControllerTest {

    @Autowired
    MockMvc mvc;

    long maquina;

    @BeforeEach
    void setUp() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        maquina = Fixtures.maquina(mvc, C1);
    }

    String bloque(String desde, String hasta) {
        return """
                {"maquinaId":%d,"desde":"%s","hasta":"%s"}""".formatted(maquina, desde, hasta);
    }

    @Test
    void declaraBloqueYLoConsulta() throws Exception {
        mvc.perform(post("/api/agenda/bloques").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content(bloque("2026-12-10T06:00:00", "2026-12-10T20:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maquinaId").value(maquina));
        mvc.perform(get("/api/agenda/bloques").param("maquinaId", String.valueOf(maquina)).with(C1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bloques", hasSize(1)))
                .andExpect(jsonPath("$.reservas", hasSize(0)));
    }

    @Test
    void bloqueSuperpuestoDevuelve409() throws Exception {
        Fixtures.bloque(mvc, C1, maquina, "2026-12-10T06:00:00", "2026-12-10T20:00:00");
        mvc.perform(post("/api/agenda/bloques").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content(bloque("2026-12-10T19:00:00", "2026-12-10T22:00:00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("RN-13")));
    }

    @Test
    void bloqueContiguoSeAcepta() throws Exception {
        Fixtures.bloque(mvc, C1, maquina, "2026-12-10T06:00:00", "2026-12-10T20:00:00");
        mvc.perform(post("/api/agenda/bloques").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content(bloque("2026-12-10T20:00:00", "2026-12-10T22:00:00")))
                .andExpect(status().isCreated());
    }

    @Test
    void intervaloInvertidoDevuelve400() throws Exception {
        mvc.perform(post("/api/agenda/bloques").with(C1).contentType(MediaType.APPLICATION_JSON)
                        .content(bloque("2026-12-10T20:00:00", "2026-12-10T06:00:00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void maquinaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/agenda/bloques").param("maquinaId", "999").with(C1)).andExpect(status().isNotFound());
    }
}
