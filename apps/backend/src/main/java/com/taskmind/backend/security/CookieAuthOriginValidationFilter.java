package com.taskmind.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.auth.interfaces.rest.AuthCookieSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CookieAuthOriginValidationFilter extends OncePerRequestFilter {
    private static final List<String> COOKIE_BACKED_AUTH_PATHS =
            List.of("/v1/auth/token/refresh", "/v1/auth/logout");

    private final ObjectMapper objectMapper;
    private final List<String> allowedOrigins;

    public CookieAuthOriginValidationFilter(
            ObjectMapper objectMapper,
            @Value("${taskmind.cors.allowed-origins:}") String allowedOrigins) {
        this.objectMapper = objectMapper;
        this.allowedOrigins = splitCommaSeparated(allowedOrigins);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || COOKIE_BACKED_AUTH_PATHS.stream()
                        .noneMatch(path -> path.equals(request.getRequestURI()))
                || !hasRefreshCookie(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String sourceOrigin = sourceOrigin(request);
        if (sourceOrigin != null && allowedOrigins.contains(sourceOrigin)) {
            filterChain.doFilter(request, response);
            return;
        }

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.FORBIDDEN,
                        "Cookie-backed refresh and logout requests must originate from the trusted frontend origin.");
        problem.setTitle("Forbidden");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private boolean hasRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }
        return Arrays.stream(cookies)
                .anyMatch(cookie -> AuthCookieSupport.REFRESH_COOKIE.equals(cookie.getName()));
    }

    private String sourceOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && !origin.isBlank()) {
            return origin;
        }
        String referer = request.getHeader(HttpHeaders.REFERER);
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            int port = uri.getPort();
            return port == -1
                    ? "%s://%s".formatted(uri.getScheme(), uri.getHost())
                    : "%s://%s:%d".formatted(uri.getScheme(), uri.getHost(), port);
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
}
