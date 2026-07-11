package com.taskmind.backend.auth.interfaces.rest;

import com.taskmind.backend.auth.application.AuthTokens;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieSupport {
    public static final String REFRESH_COOKIE = "taskmind_refresh";

    private final Duration refreshTtl;
    private final boolean secure;
    private final String sameSite;

    AuthCookieSupport(
            @Value("${taskmind.auth.jwt.refresh-ttl:P30D}") Duration refreshTtl,
            @Value("${taskmind.auth.cookies.secure:false}") boolean secure,
            @Value("${taskmind.auth.cookies.same-site:Lax}") String sameSite) {
        this.refreshTtl = refreshTtl;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    ResponseCookie refreshCookie(AuthTokens tokens) {
        return ResponseCookie.from(REFRESH_COOKIE, tokens.refreshToken())
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/v1/auth")
                .maxAge(refreshTtl)
                .build();
    }

    ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/v1/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
