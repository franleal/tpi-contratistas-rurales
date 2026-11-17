package ar.edu.tpi.contratistas;

import ar.edu.tpi.commons.excepcion.ConflictoEstadoException;
import ar.edu.tpi.contratistas.dominio.BloqueDisponibilidad;
import ar.edu.tpi.contratistas.dominio.Contratista;
import ar.edu.tpi.contratistas.dominio.EstadoReserva;
import ar.edu.tpi.contratistas.dominio.Maquina;
import ar.edu.tpi.contratistas.dominio.TipoMaquina;
import ar.edu.tpi.contratistas.repositorio.BloqueDisponibilidadRepository;
import ar.edu.tpi.contratistas.repositorio.ContratistaRepository;
import ar.edu.tpi.contratistas.repositorio.MaquinaRepository;
import ar.edu.tpi.contratistas.repositorio.ReservaRepository;
import ar.edu.tpi.contratistas.servicio.AgendaService;
import ar.edu.tpi.contratistas.servicio.ReservaService;
import ar.edu.tpi.contratistas.web.dto.BloqueRequest;
import ar.edu.tpi.contratistas.web.dto.ReservaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tarjeta 03-12 (RN-13, D-08): con varias operaciones simultáneas sobre la misma máquina,
 * el bloqueo pesimista hace que solo una gane y el resto reciba 409. Sin el bloqueo, todas
 * podrían pasar el control de superposición antes de que alguna guarde.
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConcurrenciaAgendaTest {

    static final int HILOS = 6;
    static final LocalDateTime D8 = LocalDateTime.of(2026, 12, 10, 8, 0);

    @Autowired
    ContratistaRepository contratistas;
    @Autowired
    MaquinaRepository maquinas;
    @Autowired
    BloqueDisponibilidadRepository bloques;
    @Autowired
    ReservaRepository reservas;
    @Autowired
    ReservaService reservaService;
    @Autowired
    AgendaService agendaService;

    Maquina maquina;

    @BeforeEach
    void setUp() {
        Contratista c = contratistas.save(new Contratista("c1", "Empresa", "30712345679"));
        Maquina m = new Maquina();
        m.setContratista(c);
        m.setTipo(TipoMaquina.PULVERIZADORA);
        m.setDescripcion("P");
        m.setAnchoM(BigDecimal.TEN);
        m.setRendimientoHaH(BigDecimal.TEN);
        m.setBaseLat(BigDecimal.ONE);
        m.setBaseLon(BigDecimal.ONE);
        maquina = maquinas.save(m);
    }

    /** Ejecuta las tareas todas juntas y devuelve cuántas terminaron bien y cuántas con 409. */
    static int[] enParalelo(List<Callable<Object>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<Object>> futuros = new ArrayList<>();
            for (Callable<Object> t : tareas) {
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return t.call();
                }));
            }
            largada.countDown();
            int ok = 0;
            int conflictos = 0;
            for (Future<Object> f : futuros) {
                try {
                    f.get();
                    ok++;
                } catch (java.util.concurrent.ExecutionException e) {
                    if (e.getCause() instanceof ConflictoEstadoException) {
                        conflictos++;
                    } else {
                        throw e;
                    }
                }
            }
            return new int[]{ok, conflictos};
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void reservasSimultaneasDelMismoIntervaloSoloGanaUna() throws Exception {
        bloques.save(new BloqueDisponibilidad(maquina, D8.minusHours(2), D8.plusHours(12)));
        List<Callable<Object>> tareas = new ArrayList<>();
        for (int i = 0; i < HILOS; i++) {
            long orden = 100 + i;
            // intervalos distintos pero todos superpuestos entre sí
            LocalDateTime desde = D8.plusMinutes(10L * i);
            tareas.add(() -> reservaService.reservar(new ReservaRequest(maquina.getId(), orden, desde,
                    desde.plusHours(3))));
        }

        int[] r = enParalelo(tareas);

        assertThat(r[0]).isEqualTo(1);
        assertThat(r[1]).isEqualTo(HILOS - 1);
        assertThat(reservas.findByMaquinaIdOrderByDesdeAsc(maquina.getId()))
                .filteredOn(x -> x.getEstado() == EstadoReserva.ACTIVA).hasSize(1);
    }

    @Test
    void reservasSimultaneasSinSuperposicionGananTodas() throws Exception {
        bloques.save(new BloqueDisponibilidad(maquina, D8, D8.plusHours(HILOS)));
        List<Callable<Object>> tareas = new ArrayList<>();
        for (int i = 0; i < HILOS; i++) {
            long orden = 100 + i;
            LocalDateTime desde = D8.plusHours(i);
            tareas.add(() -> reservaService.reservar(new ReservaRequest(maquina.getId(), orden, desde,
                    desde.plusHours(1))));
        }

        int[] r = enParalelo(tareas);

        assertThat(r[0]).isEqualTo(HILOS);
        assertThat(r[1]).isZero();
    }

    @Test
    void bloquesSimultaneosSuperpuestosSoloSeGuardaUno() throws Exception {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("c1").build();
        List<Callable<Object>> tareas = new ArrayList<>();
        for (int i = 0; i < HILOS; i++) {
            LocalDateTime desde = D8.plusMinutes(5L * i);
            tareas.add(() -> {
                SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
                try {
                    return agendaService.declarar(new BloqueRequest(maquina.getId(), desde, desde.plusHours(4)));
                } finally {
                    SecurityContextHolder.clearContext();
                }
            });
        }

        int[] r = enParalelo(tareas);

        assertThat(r[0]).isEqualTo(1);
        assertThat(r[1]).isEqualTo(HILOS - 1);
        assertThat(bloques.findByMaquinaIdOrderByDesdeAsc(maquina.getId())).hasSize(1);
    }
}
