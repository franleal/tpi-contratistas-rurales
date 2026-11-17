package ar.edu.tpi.campos.repositorio;

import ar.edu.tpi.campos.dominio.Productor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductorRepository extends JpaRepository<Productor, Long> {

    Optional<Productor> findBySub(String sub);
}
