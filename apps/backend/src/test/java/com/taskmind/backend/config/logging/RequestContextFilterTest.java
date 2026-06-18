package com.taskmind.backend.config.logging;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestContextFilterTest {
    private final RequestLoggingProperties properties =
            new RequestLoggingProperties(null, null, false);
    private final RequestContextFilter filter = new RequestContextFilter(properties);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void createsCorrelationIdWhenAbsentAndEmitsResponseHeader()
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.correlationId).isNotBlank();
        assertThat(response.getHeader(RequestCorrelation.HEADER_NAME))
                .isEqualTo(chain.correlationId);
    }

    @Test
    void preservesInboundCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(RequestCorrelation.HEADER_NAME, " inbound-correlation ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        CapturingFilterChain chain = new CapturingFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.correlationId).isEqualTo("inbound-correlation");
        assertThat(response.getHeader(RequestCorrelation.HEADER_NAME))
                .isEqualTo("inbound-correlation");
    }

    @Test
    void clearsMdcAfterRequestCompletion() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new CapturingFilterChain());

        assertThat(MDC.get(RequestCorrelation.MDC_KEY)).isNull();
    }

    private static class CapturingFilterChain extends MockFilterChain {
        private String correlationId;

        @Override
        public void doFilter(
                jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                throws IOException, ServletException {
            correlationId = MDC.get(RequestCorrelation.MDC_KEY);
            super.doFilter(request, response);
        }
    }
}
