package ar.edu.tpi.trabajos;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** "Ahora" fijo para las pruebas de postulación: 10/12/2026 06:00 en Argentina. */
@TestConfiguration
public class RelojFijo {

    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 12, 10, 6, 0);

    @Bean
    @Primary
    Clock relojFijo() {
        ZoneId ar = ZoneId.of("America/Argentina/Buenos_Aires");
        return Clock.fixed(AHORA.atZone(ar).toInstant(), ar);
    }
}
