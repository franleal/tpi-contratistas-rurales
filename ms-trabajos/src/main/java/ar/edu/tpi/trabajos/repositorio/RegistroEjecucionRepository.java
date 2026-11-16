package ar.edu.tpi.trabajos.repositorio;

import ar.edu.tpi.trabajos.dominio.RegistroEjecucion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistroEjecucionRepository extends JpaRepository<RegistroEjecucion, Long> {

    @EntityGraph(attributePaths = "fotos")
    Optional<RegistroEjecucion> findByOrdenId(Long ordenId);
}
