package com.taskmind.backend.auth.interfaces.rest;

import com.taskmind.backend.auth.application.*;
import com.taskmind.backend.auth.interfaces.rest.dto.*;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/auth")
@Validated
public class AuthController {
    private final AuthApplicationService authApplicationService;
    private final AuthCookieSupport authCookieSupport;

    public AuthController(AuthApplicationService authApplicationService, AuthCookieSupport authCookieSupport) {
        this.authApplicationService = authApplicationService;
        this.authCookieSupport = authCookieSupport;
    }

    @PostMapping("/signup/email")
    public ResponseEntity<Void> signupEmail(@Valid @RequestBody SignupEmailRequest request) {
        authApplicationService.signupEmail(
                new SignupEmailCommand(request.email(), request.password(), request.displayName()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthTokensResponse> verify(@Valid @RequestBody VerifyOtpRequest request) {
        return response(
                authApplicationService.verifyOtp(
                        new VerifyOtpCommand(request.email(), request.otp())));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokensResponse> login(@Valid @RequestBody LoginRequest request) {
        return response(
                authApplicationService.login(
                        new LoginCommand(request.email(), request.password())));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<AuthTokensResponse> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(name = AuthCookieSupport.REFRESH_COOKIE, required = false) String refreshCookie) {
        String refreshToken = Optional.ofNullable(refreshCookie).orElseGet(() -> request == null ? null : request.refreshToken());
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh session.");
        }
        return response(authApplicationService.refresh(new RefreshTokenCommand(refreshToken)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) LogoutRequest request,
            @CookieValue(name = AuthCookieSupport.REFRESH_COOKIE, required = false) String refreshCookie) {
        String refreshToken = Optional.ofNullable(refreshCookie).orElseGet(() -> request == null ? null : request.refreshToken());
        if (refreshToken != null && !refreshToken.isBlank()) {
            authApplicationService.logout(new LogoutCommand(refreshToken));
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieSupport.clearRefreshCookie().toString())
                .build();
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        AuthUserView user = authApplicationService.me(UUID.fromString(authentication.getName()));
        return new AuthUserResponse(
                user.userId(),
                user.email(),
                user.displayName(),
                user.onboardingCompleted(),
                user.onboardingWorkspaceType(),
                user.onboardingPlanningStyle());
    }

    private ResponseEntity<AuthTokensResponse> response(AuthTokens t) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieSupport.refreshCookie(t).toString())
                .body(new AuthTokensResponse(t.accessToken(), t.tokenType(), t.expiresInSeconds()));
    }
}
