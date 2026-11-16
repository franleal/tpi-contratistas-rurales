package ar.edu.tpi.contratistas.repositorio;

import ar.edu.tpi.contratistas.dominio.Maquina;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    @EntityGraph(attributePaths = "contratista")
    Optional<Maquina> findWithContratistaById(Long id);

    List<Maquina> findByContratistaSubOrderByIdAsc(String sub);
}
