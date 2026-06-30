package com.taskmind.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.ratelimit.RateLimitFilter;
import com.taskmind.backend.security.internal.InternalServiceTokenFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
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
        "/v1/auth/logout",
        "/v1/integrations/jira/oauth/callback",
        "/v1/integrations/github/oauth/callback"
    };

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtClaimAuthenticationConverter jwtConverter,
            ObjectMapper objectMapper,
            ObjectProvider<RateLimitFilter> rateLimitFilter,
            InternalServiceTokenFilter internalServiceTokenFilter)
            throws Exception {
        http.addFilterBefore(internalServiceTokenFilter, BearerTokenAuthenticationFilter.class);
        rateLimitFilter.ifAvailable(
                filter -> http.addFilterAfter(filter, BearerTokenAuthenticationFilter.class));

        return http.cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers("/api/health")
                                        .permitAll()
                                        .requestMatchers(PUBLIC_AUTH_ROUTES)
                                        .permitAll()
                                        .requestMatchers("/v1/**", "/internal/**")
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

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${taskmind.cors.allowed-origins:}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(splitCommaSeparated(allowedOrigins));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Location"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/v1/**", configuration);
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private List<String> splitCommaSeparated(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    private void writeProblemDetail(
            HttpServletResponse response,
            HttpStatus status,
            String title,
            String detail,
            ObjectMapper objectMapper)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
