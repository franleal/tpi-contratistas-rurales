package ar.edu.tpi.contratistas.repositorio;

import ar.edu.tpi.contratistas.dominio.BloqueDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BloqueDisponibilidadRepository extends JpaRepository<BloqueDisponibilidad, Long> {

    List<BloqueDisponibilidad> findByMaquinaIdOrderByDesdeAsc(Long maquinaId);

    /** Bloques que se superponen con [desde, hasta): a.desde &lt; hasta y desde &lt; a.hasta. */
    @Query("""
            select count(b) > 0 from BloqueDisponibilidad b
            where b.maquina.id = :maquinaId and b.desde < :hasta and :desde < b.hasta""")
    boolean existeSuperpuesto(@Param("maquinaId") Long maquinaId, @Param("desde") LocalDateTime desde,
                              @Param("hasta") LocalDateTime hasta);

    /** Bloque que contiene completo el intervalo [desde, hasta). */
    @Query("""
            select b from BloqueDisponibilidad b
            where b.maquina.id = :maquinaId and b.desde <= :desde and :hasta <= b.hasta""")
    Optional<BloqueDisponibilidad> buscarQueContiene(@Param("maquinaId") Long maquinaId,
                                                     @Param("desde") LocalDateTime desde,
                                                     @Param("hasta") LocalDateTime hasta);
}
