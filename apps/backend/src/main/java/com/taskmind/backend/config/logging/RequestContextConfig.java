package com.taskmind.backend.config.logging;

import com.taskmind.backend.ratelimit.ClientIpResolver;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableConfigurationProperties(RequestLoggingProperties.class)
public class RequestContextConfig {
    @Bean
    FilterRegistrationBean<RequestContextFilter> requestContextFilterRegistration(
            RequestLoggingProperties properties, ClientIpResolver clientIpResolver) {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestContextFilter(properties, clientIpResolver));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
