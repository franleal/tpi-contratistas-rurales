package ar.edu.tpi.trabajos.cliente;

import ar.edu.tpi.trabajos.dominio.Zona;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

/** Cliente de GET /internal/lotes/{id} (docs/CONTRATOS.md). */
@Component
public class CamposClient {

    static final String SERVICIO = "ms-campos";

    private final RestClient http;

    public CamposClient(@Qualifier("camposRestClient") RestClient http) {
        this.http = http;
    }

    public record Lote(Long id, String productorSub, String nombre, BigDecimal lat, BigDecimal lon,
                       BigDecimal superficieHa, String provincia, String departamento, Zona zona,
                       List<String> categoriasPermitidas, boolean admiteOrdenes) {
    }

    public Lote lote(Long id) {
        return ErroresInternos.llamar(SERVICIO,
                () -> http.get().uri("/internal/lotes/{id}", id).retrieve().body(Lote.class));
    }
}
