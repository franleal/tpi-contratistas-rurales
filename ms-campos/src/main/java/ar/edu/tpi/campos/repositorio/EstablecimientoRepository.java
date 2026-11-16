package ar.edu.tpi.campos.repositorio;

import ar.edu.tpi.campos.dominio.Establecimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstablecimientoRepository extends JpaRepository<Establecimiento, Long> {

    List<Establecimiento> findByProductorSubOrderByIdAsc(String sub);
}
