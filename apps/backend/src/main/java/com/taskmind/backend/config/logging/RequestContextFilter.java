package com.taskmind.backend.config.logging;

import com.taskmind.backend.ratelimit.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestContextFilter extends OncePerRequestFilter {
    private final RequestLoggingProperties properties;
    private final ClientIpResolver clientIpResolver;

    public RequestContextFilter(
            RequestLoggingProperties properties, ClientIpResolver clientIpResolver) {
        this.properties = properties;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId =
                RequestCorrelation.normalizeOrGenerate(
                        request.getHeader(properties.correlationHeader()));
        MDC.put(properties.mdcKey(), correlationId);
        MDC.put(ClientIpResolver.MDC_KEY, clientIpResolver.resolve(request));
        response.setHeader(properties.correlationHeader(), correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(properties.mdcKey());
            MDC.remove(ClientIpResolver.MDC_KEY);
        }
    }
}
