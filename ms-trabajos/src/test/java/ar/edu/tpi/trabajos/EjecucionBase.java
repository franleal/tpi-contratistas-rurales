package ar.edu.tpi.trabajos;

import ar.edu.tpi.trabajos.cliente.ContratistasClient;
import ar.edu.tpi.trabajos.cliente.OpenMeteoClient;
import ar.edu.tpi.trabajos.dominio.Asignacion;
import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import ar.edu.tpi.trabajos.dominio.Postulacion;
import ar.edu.tpi.trabajos.dominio.Zona;
import ar.edu.tpi.trabajos.repositorio.AsignacionRepository;
import ar.edu.tpi.trabajos.repositorio.OrdenTrabajoRepository;
import ar.edu.tpi.trabajos.repositorio.PostulacionRepository;
import ar.edu.tpi.trabajos.repositorio.UmbralClimaticoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Base para las pruebas de ejecución: orden ASIGNADA a c1 el 09/12 de 08:00 a 12:00 (antes de "ahora"). */
abstract class EjecucionBase {

    static final LocalDateTime INICIO = RelojFijo.AHORA.minusDays(1).withHour(8);

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdenTrabajoRepository ordenes;
    @Autowired
    PostulacionRepository postulaciones;
    @Autowired
    AsignacionRepository asignaciones;
    @Autowired
    UmbralClimaticoRepository umbrales;
    @MockitoBean
    ContratistasClient contratistas;
    @MockitoBean
    OpenMeteoClient meteo;

    long orden;

    long ordenAsignada(String productor, String contratista) {
        umbrales.save(Climas.umbral());
        OrdenTrabajo o = ordenes.save(Datos.orden(productor, Zona.LIBRE, LocalDate.of(2026, 12, 14),
                EstadoOrden.ASIGNADA));
        Postulacion p = new Postulacion();
        p.setOrden(o);
        p.setContratistaSub(contratista);
        p.setMaquinaId(3L);
        p.setAplicadorId(4L);
        p.setInicio(INICIO);
        p.setFin(INICIO.plusHours(4));
        p.setOcupadoDesde(INICIO);
        p.setHorasAplicacion(new BigDecimal("4"));
        p.setTrasladoH(BigDecimal.ZERO);
        p.setPrecioHa(new BigDecimal("100"));
        p.setEstado(EstadoPostulacion.ASIGNADA);
        postulaciones.save(p);
        Asignacion a = new Asignacion();
        a.setOrden(o);
        a.setPostulacion(p);
        a.setReservaId(50L);
        asignaciones.save(a);
        return o.getId();
    }

    ResultActions registrar(RequestPostProcessor quien, LocalDateTime inicio, LocalDateTime fin, String ha)
            throws Exception {
        return mvc.perform(post("/api/ordenes/" + orden + "/ejecucion").with(quien)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"inicio\":\"%s\",\"fin\":\"%s\",\"hectareas\":%s}".formatted(inicio, fin, ha)));
    }
}
