package com.taskmind.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabledModeDoesNotConsumeABucket() throws Exception {
        RateLimitProperties properties = propertiesWithLimits(1, 1);
        properties.setEnabled(false);
        AtomicBoolean consumed = new AtomicBoolean(false);
        RateLimitFilter filter =
                new RateLimitFilter(
                        properties,
                        (key, bucket) -> {
                            consumed.set(true);
                            return RateLimitService.RateLimitDecision.rejected(bucket.capacity(), 60);
                        },
                        new ObjectMapper());

        MockHttpServletResponse response = execute(filter, request("/v1/tasks"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(consumed).isFalse();
    }

    @Test
    void anonymousRequestsUseIpBucketLimits() throws Exception {
        RateLimitProperties properties = propertiesWithLimits(1, 10);
        InMemoryRateLimitService service = new InMemoryRateLimitService();
        RateLimitFilter filter = new RateLimitFilter(properties, service, new ObjectMapper());

        assertThat(execute(filter, request("/v1/tasks")).getStatus()).isEqualTo(200);
        MockHttpServletResponse limited = execute(filter, request("/v1/tasks"));

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(service.lastKey).isEqualTo("taskmind:ratelimit:ip:127.0.0.1");
    }

    @Test
    void authenticatedRequestsUseUserBucketLimits() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("user-123", "n/a", "ROLE_USER"));
        RateLimitProperties properties = propertiesWithLimits(10, 1);
        InMemoryRateLimitService service = new InMemoryRateLimitService();
        RateLimitFilter filter = new RateLimitFilter(properties, service, new ObjectMapper());

        assertThat(execute(filter, request("/v1/tasks")).getStatus()).isEqualTo(200);
        MockHttpServletResponse limited = execute(filter, request("/v1/tasks"));

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(service.lastKey).isEqualTo("taskmind:ratelimit:user:user-123");
    }

    @Test
    void specBreakdownDraftRequestsUseAiHeavyBucketLimits() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("user-123", "n/a", "ROLE_USER"));
        RateLimitProperties properties = propertiesWithLimits(10, 10);
        properties.setAiHeavy(new RateLimitProperties.Bucket(1, Duration.ofMinutes(1)));
        InMemoryRateLimitService service = new InMemoryRateLimitService();
        RateLimitFilter filter = new RateLimitFilter(properties, service, new ObjectMapper());

        assertThat(execute(filter, request("/v1/spec-breakdown/drafts")).getStatus()).isEqualTo(200);
        MockHttpServletResponse limited = execute(filter, request("/v1/spec-breakdown/drafts"));

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(service.lastKey).isEqualTo("taskmind:ratelimit:ai-heavy:user-123");
    }

    @Test
    void limitExceededResponseContainsStatusAndHeaders() throws Exception {
        RateLimitProperties properties = propertiesWithLimits(0, 0);
        RateLimitFilter filter =
                new RateLimitFilter(
                        properties,
                        (key, bucket) -> RateLimitService.RateLimitDecision.rejected(2, 37),
                        new ObjectMapper());

        MockHttpServletResponse response = execute(filter, request("/v1/auth/login"));

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader(RateLimitFilter.LIMIT_HEADER)).isEqualTo("2");
        assertThat(response.getHeader(RateLimitFilter.REMAINING_HEADER)).isEqualTo("0");
        assertThat(response.getHeader(RateLimitFilter.RETRY_AFTER_HEADER)).isEqualTo("37");
        assertThat(response.getContentType()).contains("application/problem+json");
        assertThat(response.getContentAsString()).contains("Too Many Requests");
    }

    @Test
    void internalEndpointsAreNotLimited() throws Exception {
        RateLimitProperties properties = propertiesWithLimits(1, 1);
        AtomicBoolean consumed = new AtomicBoolean(false);
        RateLimitFilter filter =
                new RateLimitFilter(
                        properties,
                        (key, bucket) -> {
                            consumed.set(true);
                            return RateLimitService.RateLimitDecision.rejected(bucket.capacity(), 60);
                        },
                        new ObjectMapper());

        MockHttpServletResponse response = execute(filter, request("/internal/relay/events"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(consumed).isFalse();
    }

    private RateLimitProperties propertiesWithLimits(long anonymousLimit, long authenticatedLimit) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setAnonymous(new RateLimitProperties.Bucket(anonymousLimit, Duration.ofMinutes(1)));
        properties.setAuthenticated(
                new RateLimitProperties.Bucket(authenticatedLimit, Duration.ofMinutes(1)));
        properties.setAuthFlow(new RateLimitProperties.Bucket(anonymousLimit, Duration.ofMinutes(1)));
        properties.setAiHeavy(new RateLimitProperties.Bucket(authenticatedLimit, Duration.ofMinutes(1)));
        return properties;
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private MockHttpServletResponse execute(RateLimitFilter filter, MockHttpServletRequest request)
            throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private static final class InMemoryRateLimitService implements RateLimitService {
        private final java.util.Map<String, Long> counts = new java.util.HashMap<>();
        private String lastKey;

        @Override
        public RateLimitDecision consume(String bucketKey, RateLimitProperties.Bucket bucket) {
            lastKey = bucketKey;
            long count = counts.merge(bucketKey, 1L, Long::sum);
            if (count > bucket.capacity()) {
                return RateLimitDecision.rejected(bucket.capacity(), bucket.refillPeriod().toSeconds());
            }
            return RateLimitDecision.allowed(bucket.capacity(), bucket.capacity() - count);
        }
    }
}
