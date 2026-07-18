package com.taskmind.ai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class NovaSecurityConfig {
    static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    @Bean
    SecurityFilterChain novaSecurityFilterChain(
            HttpSecurity http, ServiceTokenFilter serviceTokenFilter) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/api/health")
                                        .permitAll()
                                        .requestMatchers("/actuator/prometheus")
                                        .hasRole("SERVICE")
                                        .requestMatchers("/internal/**")
                                        .hasRole("SERVICE")
                                        .anyRequest()
                                        .denyAll())
                .addFilterBefore(serviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    ServiceTokenFilter serviceTokenFilter(
            @Value("${taskmind.ai.service-token}") String serviceToken) {
        return new ServiceTokenFilter(serviceToken);
    }

    static class ServiceTokenFilter extends OncePerRequestFilter {
        private final String serviceToken;

        ServiceTokenFilter(String serviceToken) {
            this.serviceToken = serviceToken;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            String suppliedToken = serviceTokenFrom(request);
            if (suppliedToken != null && matches(suppliedToken)) {
                SecurityContextHolder.getContext().setAuthentication(new ServiceAuthenticationToken());
            }
            try {
                filterChain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }

        private String serviceTokenFrom(HttpServletRequest request) {
            String headerToken = request.getHeader(SERVICE_TOKEN_HEADER);
            if (headerToken != null && !headerToken.isBlank()) {
                return headerToken;
            }
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                return authorization.substring("Bearer ".length());
            }
            return null;
        }

        private boolean matches(String suppliedToken) {
            return MessageDigest.isEqual(
                    serviceToken.getBytes(StandardCharsets.UTF_8), suppliedToken.getBytes(StandardCharsets.UTF_8));
        }
    }
}
