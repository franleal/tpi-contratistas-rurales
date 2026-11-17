package ar.edu.tpi.contratistas.repositorio;

import ar.edu.tpi.contratistas.dominio.Maquina;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    @EntityGraph(attributePaths = "contratista")
    Optional<Maquina> findWithContratistaById(Long id);

    List<Maquina> findByContratistaSubOrderByIdAsc(String sub);

    /**
     * SELECT ... FOR UPDATE sobre la máquina (D-08): serializa la declaración de bloques y la
     * creación de reservas de una misma máquina, así dos operaciones concurrentes no pueden
     * pasar ambas el control de superposición.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Maquina m join fetch m.contratista where m.id = :id")
    Optional<Maquina> bloquear(@Param("id") Long id);
}
