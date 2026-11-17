package ar.edu.tpi.trabajos.repositorio;

import ar.edu.tpi.trabajos.dominio.Asignacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    @EntityGraph(attributePaths = "postulacion")
    Optional<Asignacion> findByOrdenId(Long ordenId);

    @EntityGraph(attributePaths = {"orden", "postulacion"})
    List<Asignacion> findByPostulacionContratistaSub(String contratistaSub);
}
