package com.taskmind.ai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
                                        .requestMatchers("/internal/**")
                                        .authenticated()
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
            String headerToken = request.getHeader(SERVICE_TOKEN_HEADER);
            String authorization = request.getHeader("Authorization");
            if (serviceToken.equals(headerToken)
                    || (authorization != null && authorization.equals("Bearer " + serviceToken))) {
                AbstractAuthenticationToken authentication =
                        new AbstractAuthenticationToken(
                                List.of(new SimpleGrantedAuthority("ROLE_SERVICE"))) {
                            @Override
                            public Object getCredentials() {
                                return serviceToken;
                            }

                            @Override
                            public Object getPrincipal() {
                                return "taskmind-service";
                            }
                        };
                authentication.setAuthenticated(true);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        }
    }
}
