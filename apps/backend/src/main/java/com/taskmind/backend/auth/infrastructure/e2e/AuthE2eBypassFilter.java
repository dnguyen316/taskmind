package com.taskmind.backend.auth.infrastructure.e2e;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthE2eBypassFilter extends OncePerRequestFilter {
    private final boolean enabled;
    public AuthE2eBypassFilter(@Value("${taskmind.auth.e2e-bypass.enabled:false}") boolean enabled) { this.enabled=enabled; }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (enabled && SecurityContextHolder.getContext().getAuthentication() == null) {
            var userId=request.getHeader("X-User-Id");
            if (userId == null || userId.isBlank()) userId=request.getHeader("X-Actor-User-Id");
            if ((userId == null || userId.isBlank()) && !request.getRequestURI().startsWith("/v1/auth")) userId="00000000-0000-0000-0000-000000000001";
            if (userId != null && !userId.isBlank()) SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
        }
        chain.doFilter(request,response);
    }
}
