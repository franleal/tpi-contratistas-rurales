package ar.edu.tpi.contratistas;

import ar.edu.tpi.contratistas.dominio.BloqueDisponibilidad;
import ar.edu.tpi.contratistas.dominio.Contratista;
import ar.edu.tpi.contratistas.dominio.Maquina;
import ar.edu.tpi.contratistas.dominio.Reserva;
import ar.edu.tpi.contratistas.dominio.TipoMaquina;
import ar.edu.tpi.contratistas.repositorio.BloqueDisponibilidadRepository;
import ar.edu.tpi.contratistas.repositorio.ContratistaRepository;
import ar.edu.tpi.contratistas.repositorio.MaquinaRepository;
import ar.edu.tpi.contratistas.repositorio.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Consultas de superposición con intervalos semiabiertos [desde, hasta). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AgendaRepositoryTest {

    static final LocalDateTime D8 = LocalDateTime.of(2026, 12, 10, 8, 0);
    static final LocalDateTime D18 = LocalDateTime.of(2026, 12, 10, 18, 0);

    @Autowired
    ContratistaRepository contratistas;
    @Autowired
    MaquinaRepository maquinas;
    @Autowired
    BloqueDisponibilidadRepository bloques;
    @Autowired
    ReservaRepository reservas;

    Maquina maquina;

    @BeforeEach
    void setUp() {
        Contratista c = contratistas.save(new Contratista("c1", "Empresa", "30712345679"));
        maquina = new Maquina();
        maquina.setContratista(c);
        maquina.setTipo(TipoMaquina.PULVERIZADORA);
        maquina.setDescripcion("P");
        maquina.setAnchoM(BigDecimal.TEN);
        maquina.setRendimientoHaH(BigDecimal.TEN);
        maquina.setBaseLat(BigDecimal.ONE);
        maquina.setBaseLon(BigDecimal.ONE);
        maquina = maquinas.save(maquina);
        bloques.save(new BloqueDisponibilidad(maquina, D8, D18));
    }

    @Test
    void bloquesContiguosNoSeSuperponen() {
        assertThat(bloques.existeSuperpuesto(maquina.getId(), D18, D18.plusHours(4))).isFalse();
        assertThat(bloques.existeSuperpuesto(maquina.getId(), D8.minusHours(2), D8)).isFalse();
        assertThat(bloques.existeSuperpuesto(maquina.getId(), D18.minusMinutes(1), D18.plusHours(1))).isTrue();
    }

    @Test
    void buscaElBloqueQueContieneElIntervalo() {
        assertThat(bloques.buscarQueContiene(maquina.getId(), D8, D18)).isPresent();
        assertThat(bloques.buscarQueContiene(maquina.getId(), D8.plusHours(2), D8.plusHours(5))).isPresent();
        assertThat(bloques.buscarQueContiene(maquina.getId(), D8.minusMinutes(30), D8.plusHours(5))).isEmpty();
    }

    @Test
    void reservaLiberadaNoOcupaLaAgenda() {
        Reserva r = reservas.save(new Reserva(maquina, 1L, D8.plusHours(1), D8.plusHours(4)));
        assertThat(reservas.existeActivaSuperpuesta(maquina.getId(), D8.plusHours(3), D8.plusHours(6))).isTrue();
        assertThat(reservas.existeActivaSuperpuesta(maquina.getId(), D8.plusHours(4), D8.plusHours(6))).isFalse();
        r.liberar();
        reservas.saveAndFlush(r);
        assertThat(reservas.existeActivaSuperpuesta(maquina.getId(), D8.plusHours(3), D8.plusHours(6))).isFalse();
    }

    @Test
    void bloqueoPesimistaDevuelveLaMaquinaConSuContratista() {
        assertThat(maquinas.bloquear(maquina.getId())).get()
                .extracting(m -> m.getContratista().getSub()).isEqualTo("c1");
    }
}
