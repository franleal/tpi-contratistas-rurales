package ar.edu.tpi.contratistas.repositorio;

import ar.edu.tpi.contratistas.dominio.EstadoHabilitacion;
import ar.edu.tpi.contratistas.dominio.Habilitacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabilitacionRepository extends JpaRepository<Habilitacion, Long> {

    List<Habilitacion> findByMaquinaId(Long maquinaId);

    List<Habilitacion> findByAplicadorId(Long aplicadorId);

    @EntityGraph(attributePaths = {"aplicador", "maquina"})
    List<Habilitacion> findByEstadoOrderByIdAsc(EstadoHabilitacion estado);

    @EntityGraph(attributePaths = {"aplicador", "maquina"})
    List<Habilitacion> findAllByOrderByIdAsc();
}
