package ar.edu.tpi.campos.repositorio;

import ar.edu.tpi.campos.dominio.ParametroZona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametroZonaRepository extends JpaRepository<ParametroZona, String> {

    Optional<ParametroZona> findByProvinciaIgnoreCase(String provincia);
}
