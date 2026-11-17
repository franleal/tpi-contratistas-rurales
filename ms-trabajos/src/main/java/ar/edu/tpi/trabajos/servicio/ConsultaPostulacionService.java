package ar.edu.tpi.trabajos.servicio;

import ar.edu.tpi.trabajos.dominio.EstadoPostulacion;
import ar.edu.tpi.trabajos.repositorio.PostulacionRepository;
import ar.edu.tpi.trabajos.web.dto.PostulacionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** F-14: postulaciones válidas de una orden propia, en el orden de RN-14. */
@Service
public class ConsultaPostulacionService {

    private final OrdenService ordenes;
    private final PostulacionRepository postulaciones;

    public ConsultaPostulacionService(OrdenService ordenes, PostulacionRepository postulaciones) {
        this.ordenes = ordenes;
        this.postulaciones = postulaciones;
    }

    @Transactional(readOnly = true)
    public List<PostulacionResponse> listar(Long ordenId) {
        ordenes.propia(ordenId);
        return postulaciones.findByOrdenIdAndEstadoOrderByInicioAscTrasladoHAscPrecioHaAscIdAsc(ordenId,
                        EstadoPostulacion.VALIDA).stream()
                .map(PostulacionResponse::de).toList();
    }
}
