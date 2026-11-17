package ar.edu.tpi.contratistas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static ar.edu.tpi.contratistas.Usuarios.C1;
import static ar.edu.tpi.contratistas.Usuarios.usuario;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InternalReservaTest {

    @Autowired
    MockMvc mvc;

    long maquina;

    @BeforeEach
    void setUp() throws Exception {
        Fixtures.empresa(mvc, C1, "30712345679");
        maquina = Fixtures.maquina(mvc, C1);
        Fixtures.bloque(mvc, C1, maquina, "2026-12-10T06:00:00", "2026-12-10T20:00:00");
    }

    ResultActions reservar(long orden, String desde, String hasta) throws Exception {
        // la asignación la hace el productor: su token llega propagado por ms-trabajos
        return mvc.perform(post("/internal/agenda/reservas").with(usuario("p1"))
                .contentType(MediaType.APPLICATION_JSON).content(Fixtures.reserva(maquina, orden, desde, hasta)));
    }

    ResultActions disponibilidad(String desde, String hasta) throws Exception {
        return mvc.perform(get("/internal/maquinas/" + maquina + "/disponibilidad")
                .param("desde", desde).param("hasta", hasta).with(C1));
    }

    @Test
    void disponibleDentroDelBloque() throws Exception {
        disponibilidad("2026-12-10T08:00:00", "2026-12-10T12:00:00")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void noDisponibleFueraDelBloque() throws Exception {
        disponibilidad("2026-12-10T18:00:00", "2026-12-10T21:00:00")
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.motivo", containsString("bloque")));
    }

    @Test
    void reservaYLuegoElIntervaloDejaDeEstarDisponible() throws Exception {
        reservar(7, "2026-12-10T08:00:00", "2026-12-10T12:00:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.ordenId").value(7));
        disponibilidad("2026-12-10T11:00:00", "2026-12-10T13:00:00")
                .andExpect(jsonPath("$.disponible").value(false))
                .andExpect(jsonPath("$.motivo", containsString("reserva")));
        disponibilidad("2026-12-10T12:00:00", "2026-12-10T13:00:00")
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void reservaSuperpuestaDevuelve409() throws Exception {
        reservar(7, "2026-12-10T08:00:00", "2026-12-10T12:00:00").andExpect(status().isCreated());
        reservar(8, "2026-12-10T10:00:00", "2026-12-10T14:00:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO"));
    }

    @Test
    void reservaFueraDeBloqueDevuelve422() throws Exception {
        reservar(7, "2026-12-11T08:00:00", "2026-12-11T12:00:00").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void liberarDejaLibreElIntervaloYEsIdempotente() throws Exception {
        long id = Fixtures.id(reservar(7, "2026-12-10T08:00:00", "2026-12-10T12:00:00")
                .andReturn().getResponse().getContentAsString());
        mvc.perform(delete("/internal/agenda/reservas/" + id).with(usuario("p1"))).andExpect(status().isNoContent());
        mvc.perform(delete("/internal/agenda/reservas/" + id).with(usuario("p1"))).andExpect(status().isNoContent());

        disponibilidad("2026-12-10T08:00:00", "2026-12-10T12:00:00").andExpect(jsonPath("$.disponible").value(true));
        reservar(8, "2026-12-10T09:00:00", "2026-12-10T11:00:00").andExpect(status().isCreated());
        mvc.perform(get("/api/agenda/bloques").param("maquinaId", String.valueOf(maquina)).with(C1))
                .andExpect(jsonPath("$.reservas[0].estado").value("LIBERADA"))
                .andExpect(jsonPath("$.reservas[1].estado").value("ACTIVA"));
    }

    @Test
    void liberarInexistenteDevuelve404() throws Exception {
        mvc.perform(delete("/internal/agenda/reservas/999").with(usuario("p1"))).andExpect(status().isNotFound());
    }

    @Test
    void reservaSinTokenDevuelve401() throws Exception {
        mvc.perform(post("/internal/agenda/reservas").contentType(MediaType.APPLICATION_JSON)
                        .content(Fixtures.reserva(maquina, 1, "2026-12-10T08:00:00", "2026-12-10T12:00:00")))
                .andExpect(status().isUnauthorized());
    }
}
