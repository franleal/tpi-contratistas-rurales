package ar.edu.tpi.trabajos.repositorio;

import ar.edu.tpi.trabajos.dominio.Receta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecetaRepository extends JpaRepository<Receta, Long> {

    @EntityGraph(attributePaths = "agronomo")
    Optional<Receta> findByOrdenId(Long ordenId);
}
