package ar.edu.tpi.trabajos.repositorio;

import ar.edu.tpi.trabajos.dominio.EstadoOrden;
import ar.edu.tpi.trabajos.dominio.OrdenTrabajo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {

    List<OrdenTrabajo> findByProductorSubOrderByIdDesc(String productorSub);

    List<OrdenTrabajo> findByEstadoInOrderByIdDesc(Collection<EstadoOrden> estados);

    /** Bloquea la orden para que dos operaciones concurrentes no la hagan avanzar dos veces. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdenTrabajo o where o.id = :id")
    Optional<OrdenTrabajo> bloquear(@Param("id") Long id);
}
