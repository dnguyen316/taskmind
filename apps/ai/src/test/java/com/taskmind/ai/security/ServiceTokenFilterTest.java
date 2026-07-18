package com.taskmind.ai.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class ServiceTokenFilterTest {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesBearerServiceTokenWithoutExposingCredentials() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer nova-secret-token");

        FilterChain assertingChain =
                (servletRequest, servletResponse) -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    assertThat(authentication).isNotNull();
                    assertThat(authentication.isAuthenticated()).isTrue();
                    assertThat(authentication.getPrincipal()).isEqualTo("taskmind-service");
                    assertThat(authentication.getAuthorities())
                            .extracting("authority")
                            .containsExactly("ROLE_SERVICE");
                    assertThat(authentication.getCredentials()).isEqualTo("[PROTECTED]");
                    assertThat(authentication.getCredentials()).isNotEqualTo("nova-secret-token");
                };

        new NovaSecurityConfig.ServiceTokenFilter("nova-secret-token")
                .doFilter(request, new MockHttpServletResponse(), assertingChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authenticatesHeaderServiceTokenWithoutExposingCredentials() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(NovaSecurityConfig.SERVICE_TOKEN_HEADER, "nova-secret-token");

        FilterChain assertingChain =
                (servletRequest, servletResponse) -> {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    assertThat(authentication).isNotNull();
                    assertThat(authentication.getCredentials()).isEqualTo("[PROTECTED]");
                    assertThat(authentication.getCredentials()).isNotEqualTo("nova-secret-token");
                };

        new NovaSecurityConfig.ServiceTokenFilter("nova-secret-token")
                .doFilter(request, new MockHttpServletResponse(), assertingChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void leavesSecurityContextEmptyWhenServiceTokenDoesNotMatch() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer wrong-token");
        request.addHeader(NovaSecurityConfig.SERVICE_TOKEN_HEADER, "wrong-token");

        new NovaSecurityConfig.ServiceTokenFilter("nova-secret-token")
                .doFilter(request, new MockHttpServletResponse(), (servletRequest, servletResponse) -> {});

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
