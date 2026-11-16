package ar.edu.tpi.trabajos.web.dto;

import ar.edu.tpi.trabajos.dominio.FotoClima;
import ar.edu.tpi.trabajos.dominio.RegistroEjecucion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EjecucionResponse(Long id, LocalDateTime inicio, LocalDateTime fin, BigDecimal hectareas,
                                boolean climaDisponible, boolean fueraDeCondiciones, LocalDateTime registradaEn,
                                LocalDateTime confirmadaEn, List<Foto> fotoClima) {

    public record Foto(LocalDateTime hora, double vientoKmh, double rafagaKmh, double precipitacionMm,
                       double temperaturaC, double humedadPct, boolean apta, String motivo) {

        static Foto de(FotoClima f) {
            return new Foto(f.getHora(), f.getVientoKmh(), f.getRafagaKmh(), f.getPrecipitacionMm(),
                    f.getTemperaturaC(), f.getHumedadPct(), f.isApta(), f.getMotivo());
        }
    }

    public static EjecucionResponse de(RegistroEjecucion r) {
        return new EjecucionResponse(r.getId(), r.getInicio(), r.getFin(), r.getHectareas(), r.isClimaDisponible(),
                r.isFueraDeCondiciones(), r.getRegistradaEn(), r.getConfirmadaEn(),
                r.getFotos().stream().map(Foto::de).toList());
    }
}
