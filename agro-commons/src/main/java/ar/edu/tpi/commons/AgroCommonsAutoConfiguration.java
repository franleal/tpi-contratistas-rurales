package ar.edu.tpi.commons;

import ar.edu.tpi.commons.error.GlobalExceptionHandler;
import ar.edu.tpi.commons.log.CorrelationIdFilter;
import ar.edu.tpi.commons.log.CorrelationIdInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

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

    /** Antes que Spring Security, para que también los 401/403 lleven el correlation ID. */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registro = new FilterRegistrationBean<>(new CorrelationIdFilter());
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registro.addUrlPatterns("/*");
        return registro;
    }

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdInterceptor correlationIdInterceptor() {
        return new CorrelationIdInterceptor();
    }
}
