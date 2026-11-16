package ar.edu.tpi.contratistas.repositorio;

import ar.edu.tpi.contratistas.dominio.Contratista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContratistaRepository extends JpaRepository<Contratista, Long> {

    Optional<Contratista> findBySub(String sub);

    boolean existsByCuit(String cuit);
}
