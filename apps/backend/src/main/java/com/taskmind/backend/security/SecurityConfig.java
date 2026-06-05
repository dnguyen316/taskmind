package com.taskmind.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    private final AuthenticatedUserResolver authenticatedUserResolver;

    public SecurityConfig(AuthenticatedUserResolver authenticatedUserResolver) {
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authenticatedUserResolver);
    }

    private static final String[] PUBLIC_AUTH_ROUTES = {
        "/v1/auth/login",
        "/v1/auth/signup/**",
        "/v1/auth/verify/**",
        "/v1/auth/oauth/**",
        "/v1/auth/password/**",
        "/v1/auth/token/refresh",
        "/v1/auth/logout"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtClaimAuthenticationConverter jwtConverter,
            ObjectMapper objectMapper)
            throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/api/health")
                                        .permitAll()
                                        .requestMatchers(PUBLIC_AUTH_ROUTES)
                                        .permitAll()
                                        .requestMatchers("/v1/**")
                                        .authenticated()
                                        .dispatcherTypeMatchers(
                                                DispatcherType.ERROR, DispatcherType.FORWARD)
                                        .permitAll()
                                        .anyRequest()
                                        .denyAll())
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                                        .authenticationEntryPoint(
                                                (request, response, authException) ->
                                                        writeProblemDetail(
                                                                response,
                                                                HttpStatus.UNAUTHORIZED,
                                                                "Unauthorized",
                                                                "Authentication is required to access this resource.",
                                                                objectMapper))
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) ->
                                                        writeProblemDetail(
                                                                response,
                                                                HttpStatus.FORBIDDEN,
                                                                "Forbidden",
                                                                "You do not have permission to access this resource.",
                                                                objectMapper)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .build();
    }

    private void writeProblemDetail(
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail,
            ObjectMapper objectMapper)
            throws IOException {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
