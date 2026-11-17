package ar.edu.tpi.contratistas.repositorio;

import ar.edu.tpi.contratistas.dominio.Aplicador;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AplicadorRepository extends JpaRepository<Aplicador, Long> {

    @EntityGraph(attributePaths = "contratista")
    Optional<Aplicador> findWithContratistaById(Long id);

    List<Aplicador> findByContratistaSubOrderByIdAsc(String sub);

    boolean existsByContratistaIdAndDocumento(Long contratistaId, String documento);
}
