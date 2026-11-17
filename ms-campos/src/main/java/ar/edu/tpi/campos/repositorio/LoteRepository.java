package ar.edu.tpi.campos.repositorio;

import ar.edu.tpi.campos.dominio.Lote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    /** Trae el lote con su establecimiento y productor para verificar la propiedad. */
    @EntityGraph(attributePaths = {"establecimiento", "establecimiento.productor"})
    Optional<Lote> findWithDuenioById(Long id);
}
