package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Postulacion;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.PostulacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static ar.edu.tpi.trabajos.Usuarios.P1;
import static ar.edu.tpi.trabajos.Usuarios.P2;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** F-14 / RN-14: inicio más temprano, luego menor traslado, luego menor precio. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConsultaPostulacionTest {

    static final LocalDateTime D8 = LocalDateTime.of(2026, 12, 11, 8, 0);

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @Autowired
    PostulacionRepository postulaciones;

    OrdenTrabajo orden;

    long postulacion(LocalDateTime inicio, String traslado, String precio, EstadoPostulacion estado) {
        Postulacion p = new Postulacion();
        p.setOrden(orden);
        p.setContratistaSub("c1");
        p.setMaquinaId(3L);
        p.setAplicadorId(4L);
        p.setInicio(inicio);
        p.setFin(inicio.plusHours(4));
        p.setOcupadoDesde(inicio);
        p.setHorasAplicacion(new BigDecimal("4"));
        p.setTrasladoH(new BigDecimal(traslado));
        p.setPrecioHa(new BigDecimal(precio));
        p.setEstado(estado);
        return postulaciones.save(p).getId();
    }

    @BeforeEach
    void setUp() {
        orden = ordenes.save(Datos.orden("p1", Zona.LIBRE, LocalDate.of(2026, 12, 14), EstadoOrden.ABIERTA));
    }

    @Test
    void ordenaPorInicioTrasladoYPrecio() throws Exception {
        long tarde = postulacion(D8.plusHours(2), "0.10", "100", EstadoPostulacion.VALIDA);
        long caraLejana = postulacion(D8, "1.50", "90", EstadoPostulacion.VALIDA);
        long cara = postulacion(D8, "0.50", "150", EstadoPostulacion.VALIDA);
        long barata = postulacion(D8, "0.50", "120", EstadoPostulacion.VALIDA);
        postulacion(D8.minusHours(1), "0", "1", EstadoPostulacion.DESCARTADA);

        mvc.perform(get("/api/ordenes/" + orden.getId() + "/postulaciones").with(P1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].id").value(barata))
                .andExpect(jsonPath("$[1].id").value(cara))
                .andExpect(jsonPath("$[2].id").value(caraLejana))
                .andExpect(jsonPath("$[3].id").value(tarde));
    }

    @Test
    void ordenAjenaDevuelve403() throws Exception {
        mvc.perform(get("/api/ordenes/" + orden.getId() + "/postulaciones").with(P2)).andExpect(status().isForbidden());
    }
}
