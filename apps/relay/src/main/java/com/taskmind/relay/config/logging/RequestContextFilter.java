package com.taskmind.relay.config.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestContextFilter extends OncePerRequestFilter {
    private final RequestLoggingProperties properties;

    public RequestContextFilter(RequestLoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId =
                RequestCorrelation.normalizeOrGenerate(request.getHeader(properties.correlationHeader()));
        MDC.put(properties.mdcKey(), correlationId);
        response.setHeader(properties.correlationHeader(), correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(properties.mdcKey());
        }
    }
}
