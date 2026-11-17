package ar.edu.tpi.contratistas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static ar.edu.tpi.contratistas.Usuarios.ADMIN;
import static ar.edu.tpi.contratistas.Usuarios.usuario;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Carga el data.sql real (fechas relativas) y verifica los casos que prepara para la demo. */
@SpringBootTest(properties = "spring.sql.init.mode=always")
@AutoConfigureMockMvc
@DirtiesContext
class DatosDemoTest {

    static final String C1 = "22222222-0000-0000-0000-000000000001";

    @Autowired
    MockMvc mvc;

    @Test
    void contratista1TieneMaquinaYAplicadorHabilitadosEnLaSemana() throws Exception {
        mvc.perform(get("/internal/maquinas/1/habilitacion").param("aplicadorId", "1")
                        .param("fecha", LocalDate.now().plusDays(3).toString()).with(usuario("x")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitado").value(true))
                .andExpect(jsonPath("$.contratistaSub").value(C1));
        mvc.perform(get("/api/agenda/bloques").param("maquinaId", "1").with(usuario(C1)))
                .andExpect(jsonPath("$.bloques", hasSize(1)));
    }

    @Test
    void carnetDelAplicador2VenceEnDosDias() throws Exception {
        mvc.perform(get("/internal/maquinas/2/habilitacion").param("aplicadorId", "2")
                        .param("fecha", LocalDate.now().plusDays(5).toString()).with(usuario("x")))
                .andExpect(jsonPath("$.habilitado").value(false))
                .andExpect(jsonPath("$.motivo", containsString("RN-02")));
    }

    @Test
    void hayUnaHabilitacionPendienteParaElAdmin() throws Exception {
        mvc.perform(get("/api/admin/habilitaciones").param("estado", "PENDIENTE").with(ADMIN))
                .andExpect(jsonPath("$", hasSize(1)));
    }
}
