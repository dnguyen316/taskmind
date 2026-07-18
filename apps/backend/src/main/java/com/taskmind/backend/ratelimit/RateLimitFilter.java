package com.taskmind.backend.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RateLimitFilter extends OncePerRequestFilter {

    public static final String LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String RETRY_AFTER_HEADER = "Retry-After";

    private final RateLimitProperties properties;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;
    private final ClientIpResolver clientIpResolver;

    public RateLimitFilter(
            RateLimitProperties properties,
            RateLimitService rateLimitService,
            ObjectMapper objectMapper,
            ClientIpResolver clientIpResolver) {
        this.properties = properties;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !properties.isEnabled()
                || matches(path, properties.getInternalPrefixes())
                || !matchesPublicApi(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        BucketSelection selection = bucketSelection(request);
        RateLimitService.RateLimitDecision decision =
                rateLimitService.consume(selection.key(), selection.bucket());
        response.setHeader(LIMIT_HEADER, Long.toString(decision.limit()));
        response.setHeader(REMAINING_HEADER, Long.toString(decision.remaining()));
        if (!decision.allowed()) {
            response.setHeader(RETRY_AFTER_HEADER, Long.toString(decision.retryAfterSeconds()));
            writeLimitExceeded(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private BucketSelection bucketSelection(HttpServletRequest request) {
        String path = request.getRequestURI();
        String prefix = properties.getKeyPrefix();
        if (matches(path, properties.getAuthFlowPaths())) {
            return new BucketSelection(
                    prefix + ":auth-flow:" + clientIpResolver.resolve(request),
                    properties.getAuthFlow());
        }
        if (matches(path, properties.getAiHeavyPaths())) {
            String identity = authenticatedIdentity();
            return new BucketSelection(
                    prefix
                            + ":ai-heavy:"
                            + (identity == null ? clientIpResolver.resolve(request) : identity),
                    properties.getAiHeavy());
        }
        String identity = authenticatedIdentity();
        if (identity != null) {
            return new BucketSelection(prefix + ":user:" + identity, properties.getAuthenticated());
        }
        return new BucketSelection(
                prefix + ":ip:" + clientIpResolver.resolve(request), properties.getAnonymous());
    }

    private String authenticatedIdentity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return authentication.getName();
    }

    private boolean matchesPublicApi(String path) {
        return properties.getPublicApiPrefixes().stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix));
    }

    private boolean matches(String path, List<String> prefixes) {
        return prefixes.stream().anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix));
    }

    private void writeLimitExceeded(HttpServletResponse response) throws IOException {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Rate limit exceeded. Retry after the number of seconds in the Retry-After header.");
        problem.setTitle("Too Many Requests");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private record BucketSelection(String key, RateLimitProperties.Bucket bucket) {}
}
