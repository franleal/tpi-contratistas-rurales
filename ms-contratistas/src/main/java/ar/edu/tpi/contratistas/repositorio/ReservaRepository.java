package ar.edu.tpi.contratistas.repositorio;

import ar.edu.tpi.contratistas.dominio.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByMaquinaIdOrderByDesdeAsc(Long maquinaId);

    /** Reservas ACTIVAS que se superponen con [desde, hasta). Las LIBERADAS no ocupan. */
    @Query("""
            select count(r) > 0 from Reserva r
            where r.maquina.id = :maquinaId and r.estado = ar.edu.tpi.contratistas.dominio.EstadoReserva.ACTIVA
              and r.desde < :hasta and :desde < r.hasta""")
    boolean existeActivaSuperpuesta(@Param("maquinaId") Long maquinaId, @Param("desde") LocalDateTime desde,
                                    @Param("hasta") LocalDateTime hasta);
}
