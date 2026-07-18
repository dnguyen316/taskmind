package com.taskmind.backend.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmind.backend.auth.application.AuthApplicationService;
import com.taskmind.backend.auth.application.AuthTokens;
import com.taskmind.backend.auth.interfaces.rest.AuthController;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AuthController.class, SecurityRouteTest.ActuatorProbeController.class})
@Import({
    SecurityConfig.class,
    JwtClaimAuthenticationConverter.class,
    com.taskmind.backend.auth.interfaces.rest.AuthCookieSupport.class,
    CookieAuthOriginValidationFilter.class,
    SecurityRouteTest.ActuatorProbeController.class
})
@TestPropertySource(
        properties = {
            "taskmind.cors.allowed-origins=http://localhost:5173",
            "taskmind.nova.service-token=test-only-nova-token"
        })
class SecurityRouteTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AuthApplicationService authApplicationService;

    @MockBean private JwtDecoder jwtDecoder;

    @Test
    void allowsUnauthenticatedLoginAndSignupRequests() throws Exception {
        mockMvc.perform(
                        post("/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        post("/v1/auth/signup/email")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allowsOtherUnauthenticatedAuthFlows() throws Exception {
        mockMvc.perform(
                        post("/v1/auth/verify")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/v1/auth/oauth/google")).andExpect(status().isNotFound());
        mockMvc.perform(post("/v1/auth/password/reset")).andExpect(status().isNotFound());
        mockMvc.perform(
                        post("/v1/auth/token/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(
                        post("/v1/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isNoContent());
    }


    @Test
    void rejectsCrossSiteCookieBackedRefreshAndLogoutRequests() throws Exception {
        Cookie refreshCookie = new Cookie("taskmind_refresh", "refresh-token");

        mockMvc.perform(
                        post("/v1/auth/token/refresh")
                                .cookie(refreshCookie)
                                .header(HttpHeaders.REFERER, "https://evil.example/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(
                        post("/v1/auth/logout")
                                .cookie(refreshCookie)
                                .header(HttpHeaders.REFERER, "https://evil.example/account")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void allowsTrustedFrontendCookieBackedRefreshAndLogoutRequests() throws Exception {
        when(authApplicationService.refresh(any()))
                .thenReturn(new AuthTokens("new-access-token", "new-refresh-token", "Bearer", 300));
        Cookie refreshCookie = new Cookie("taskmind_refresh", "refresh-token");

        mockMvc.perform(
                        post("/v1/auth/token/refresh")
                                .cookie(refreshCookie)
                                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/v1/auth/logout")
                                .cookie(refreshCookie)
                                .header(HttpHeaders.REFERER, "http://localhost:5173/settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void allowsLocalFrontendCorsPreflightRequests() throws Exception {
        mockMvc.perform(
                        options("/v1/tasks")
                                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                                .header(
                                        HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                                        HttpMethod.GET.name()))
                .andExpect(status().isOk())
                .andExpect(
                        header().string(
                                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                                        "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void requiresAuthenticationForCurrentUser() throws Exception {
        mockMvc.perform(get("/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void requiresAuthenticationForTaskAndProjectRoutes() throws Exception {
        mockMvc.perform(get("/v1/tasks")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/v1/projects")).andExpect(status().isUnauthorized());
    }


    @Test
    void protectsPrometheusScrapesWithServiceToken() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/prometheus").header("X-Service-Token", "test-only-nova-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("# HELP taskmind_test_metric Test metric\n"));
    }

    @Test
    void blocksNonPrometheusActuatorEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void deniesUnknownRoutes() throws Exception {
        mockMvc.perform(get("/unknown")).andExpect(status().isUnauthorized());
    }

    @RestController
    static class ActuatorProbeController {

        @GetMapping("/actuator/prometheus")
        String prometheus() {
            return "# HELP taskmind_test_metric Test metric\n";
        }

        @GetMapping("/actuator/env")
        String env() {
            return "secret";
        }
    }
}
