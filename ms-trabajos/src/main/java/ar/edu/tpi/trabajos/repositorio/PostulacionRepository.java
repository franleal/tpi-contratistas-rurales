package ar.edu.tpi.trabajos.repositorio;

import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.dominio.Postulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    /** RN-14: inicio más temprano, luego menor traslado y luego menor precio por hectárea. */
    List<Postulacion> findByOrdenIdAndEstadoOrderByInicioAscTrasladoHAscPrecioHaAscIdAsc(Long ordenId,
                                                                                         EstadoPostulacion estado);

    List<Postulacion> findByOrdenId(Long ordenId);
}
