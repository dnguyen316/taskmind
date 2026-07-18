package com.taskmind.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.auth.application.AuthApplicationService;
import com.taskmind.backend.auth.application.AuthTokens;
import com.taskmind.backend.auth.interfaces.rest.AuthController;
import com.taskmind.backend.auth.interfaces.rest.AuthCookieSupport;
import com.taskmind.backend.security.CookieAuthOriginValidationFilter;
import com.taskmind.backend.security.JwtClaimAuthenticationConverter;
import com.taskmind.backend.security.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@Import({
    SecurityConfig.class,
    JwtClaimAuthenticationConverter.class,
    AuthCookieSupport.class,
    CookieAuthOriginValidationFilter.class
})
@ActiveProfiles("prod")
@TestPropertySource(
        properties = {
            "taskmind.auth.cookies.secure=true",
            "taskmind.auth.cookies.same-site=Lax",
            "taskmind.cors.allowed-origins=https://app.taskmind.example",
            "taskmind.nova.service-token=test-only-nova-token"
        })
class ProdRefreshCookieContractTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;

    @MockBean private AuthApplicationService authApplicationService;
    @MockBean private JwtDecoder jwtDecoder;

    @Test
    void prodRefreshEmitsSecureHttpOnlyScopedSameSiteCookieWithoutRefreshTokenInBody() throws Exception {
        when(authApplicationService.refresh(any()))
                .thenReturn(new AuthTokens("new-access-token", "new-refresh-token", "Bearer", 300));

        var result = mvc.perform(post("/v1/auth/token/refresh")
                        .cookie(new Cookie(AuthCookieSupport.REFRESH_COOKIE, "old-refresh-token"))
                        .header(HttpHeaders.ORIGIN, "https://app.taskmind.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("taskmind_refresh=new-refresh-token");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Lax");
        assertThat(setCookie).contains("Path=/v1/auth");

        var body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("accessToken").asText()).isEqualTo("new-access-token");
        assertThat(body.has("refreshToken")).isFalse();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("new-refresh-token");
    }
}
