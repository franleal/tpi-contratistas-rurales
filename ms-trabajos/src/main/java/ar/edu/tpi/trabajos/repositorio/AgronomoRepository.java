package ar.edu.tpi.trabajos.repositorio;

import ar.edu.tpi.trabajos.dominio.Agronomo;
import ar.edu.tpi.trabajos.dominio.EstadoMatricula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgronomoRepository extends JpaRepository<Agronomo, Long> {

    Optional<Agronomo> findBySub(String sub);

    List<Agronomo> findByEstadoOrderByIdAsc(EstadoMatricula estado);

    List<Agronomo> findAllByOrderByIdAsc();
}
