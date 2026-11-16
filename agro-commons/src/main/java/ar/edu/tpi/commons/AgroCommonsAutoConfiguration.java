package ar.edu.tpi.commons;

import ar.edu.tpi.commons.error.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Registra los componentes de agro-commons en cada servicio sin que tengan que
 * escanear el paquete {@code ar.edu.tpi.commons}. Se declara en
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AgroCommonsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
