package ar.edu.tpi.contratistas.servicio;

import ar.edu.tpi.contratistas.dominio.EstadoHabilitacion;
import ar.edu.tpi.contratistas.dominio.Habilitacion;
import org.springframework.stereotype.Component;

/** Validación documental: vale lo que resolvió el ADMIN (F-08). */
@Component
public class ValidadorDocumental implements ValidadorHabilitaciones {

    @Override
    public boolean estaValidada(Habilitacion habilitacion) {
        return habilitacion.getEstado() == EstadoHabilitacion.VALIDADA;
    }
}
