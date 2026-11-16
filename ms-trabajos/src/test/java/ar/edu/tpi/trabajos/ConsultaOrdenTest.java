package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.cliente.CamposClient;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static ar.edu.tpi.trabajos.Usuarios.A1;
import static ar.edu.tpi.trabajos.Usuarios.C1;
import static ar.edu.tpi.trabajos.Usuarios.P1;
import static ar.edu.tpi.trabajos.Usuarios.P2;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConsultaOrdenTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @MockitoBean
    CamposClient campos;

    long pendienteP1;

    static OrdenTrabajo orden(String duenio, EstadoOrden estado) {
        OrdenTrabajo o = new OrdenTrabajo();
        o.setProductorSub(duenio);
        o.setLoteId(1L);
        o.setLoteLat(new BigDecimal("-31.4"));
        o.setLoteLon(new BigDecimal("-64.2"));
        o.setSuperficieHa(BigDecimal.TEN);
        o.setZona(Zona.LIBRE);
        o.setProvincia("Córdoba");
        o.setFechaLimite(LocalDate.now().plusDays(10));
        o.setEstado(estado);
        return o;
    }

    @BeforeEach
    void setUp() {
        pendienteP1 = ordenes.save(orden("p1", EstadoOrden.RECETA_PENDIENTE)).getId();
        ordenes.save(orden("p1", EstadoOrden.ABIERTA));
        ordenes.save(orden("p2", EstadoOrden.ABIERTA));
        ordenes.save(orden("p2", EstadoOrden.RECETA_PENDIENTE));
    }

    @Test
    void elProductorVeSoloLasPropias() throws Exception {
        mvc.perform(get("/api/ordenes").with(P1)).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/ordenes").param("estado", "ABIERTA").with(P1)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void elAgronomoVeLasQueEsperanReceta() throws Exception {
        mvc.perform(get("/api/ordenes").with(A1))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].estado").value("RECETA_PENDIENTE"));
    }

    @Test
    void elContratistaVeLasAbiertas() throws Exception {
        mvc.perform(get("/api/ordenes").with(C1))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].estado").value("ABIERTA"));
    }

    @Test
    void detalleDeOrdenPropiaSinReceta() throws Exception {
        mvc.perform(get("/api/ordenes/" + pendienteP1).with(P1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orden.estado").value("RECETA_PENDIENTE"))
                .andExpect(jsonPath("$.receta").value(nullValue()));
    }

    @Test
    void detalleDeOrdenAjenaDevuelve403() throws Exception {
        mvc.perform(get("/api/ordenes/" + pendienteP1).with(P2)).andExpect(status().isForbidden());
    }

    @Test
    void elAgronomoPuedeVerElDetalle() throws Exception {
        mvc.perform(get("/api/ordenes/" + pendienteP1).with(A1)).andExpect(status().isOk());
    }

    @Test
    void ordenInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/ordenes/999").with(P1)).andExpect(status().isNotFound());
    }
}
