package ar.edu.tpi.campos.servicio;

import ar.edu.tpi.campos.dominio.Establecimiento;
import ar.edu.tpi.campos.dominio.Productor;
import ar.edu.tpi.campos.repositorio.EstablecimientoRepository;
import ar.edu.tpi.campos.repositorio.ProductorRepository;
import ar.edu.tpi.campos.web.dto.EstablecimientoRequest;
import ar.edu.tpi.campos.web.dto.EstablecimientoResponse;
import ar.edu.tpi.commons.excepcion.RecursoNoEncontradoException;
import ar.edu.tpi.commons.seguridad.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** F-01: establecimientos del productor autenticado. */
@Service
public class EstablecimientoService {

    private final EstablecimientoRepository establecimientos;
    private final ProductorRepository productores;

    public EstablecimientoService(EstablecimientoRepository establecimientos, ProductorRepository productores) {
        this.establecimientos = establecimientos;
        this.productores = productores;
    }

    /** Crea el establecimiento; en el primer uso da de alta el perfil del productor con su sub. */
    @Transactional
    public EstablecimientoResponse crear(EstablecimientoRequest req) {
        Productor productor = productorActual();
        Establecimiento e = establecimientos.save(
                new Establecimiento(productor, req.nombre().trim(), req.provincia().trim()));
        return EstablecimientoResponse.de(e);
    }

    @Transactional(readOnly = true)
    public List<EstablecimientoResponse> listarPropios() {
        return establecimientos.findByProductorSubOrderByIdAsc(CurrentUser.sub()).stream()
                .map(EstablecimientoResponse::de)
                .toList();
    }

    /** Establecimiento propio: 404 si no existe y 403 si es de otro productor. */
    @Transactional(readOnly = true)
    public Establecimiento propio(Long id) {
        Establecimiento e = establecimientos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el establecimiento " + id));
        CurrentUser.verificarDuenio(e.getProductor().getSub(), "el establecimiento " + id);
        return e;
    }

    private Productor productorActual() {
        String sub = CurrentUser.sub();
        return productores.findBySub(sub)
                .orElseGet(() -> productores.save(new Productor(sub, CurrentUser.nombre(), CurrentUser.email())));
    }
}
