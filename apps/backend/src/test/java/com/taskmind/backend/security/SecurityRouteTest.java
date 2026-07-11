package com.taskmind.backend.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmind.backend.auth.application.AuthApplicationService;
import com.taskmind.backend.auth.interfaces.rest.AuthController;
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
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtClaimAuthenticationConverter.class, com.taskmind.backend.auth.interfaces.rest.AuthCookieSupport.class})
@TestPropertySource(properties = "taskmind.cors.allowed-origins=http://localhost:5173")
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
    void deniesUnknownRoutes() throws Exception {
        mockMvc.perform(get("/unknown")).andExpect(status().isUnauthorized());
    }
}
