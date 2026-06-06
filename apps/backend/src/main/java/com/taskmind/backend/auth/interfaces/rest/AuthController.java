package com.taskmind.backend.auth.interfaces.rest;

import com.taskmind.backend.auth.application.*;
import com.taskmind.backend.auth.interfaces.rest.dto.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@Validated
public class AuthController {
    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/signup/email")
    public ResponseEntity<Void> signupEmail(@Valid @RequestBody SignupEmailRequest request) {
        authApplicationService.signupEmail(
                new SignupEmailCommand(request.email(), request.password(), request.displayName()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/verify")
    public AuthTokensResponse verify(@Valid @RequestBody VerifyOtpRequest request) {
        return response(
                authApplicationService.verifyOtp(
                        new VerifyOtpCommand(request.email(), request.otp())));
    }

    @PostMapping("/login")
    public AuthTokensResponse login(@Valid @RequestBody LoginRequest request) {
        return response(
                authApplicationService.login(
                        new LoginCommand(request.email(), request.password())));
    }

    @PostMapping("/token/refresh")
    public AuthTokensResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return response(
                authApplicationService.refresh(new RefreshTokenCommand(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authApplicationService.logout(new LogoutCommand(request.refreshToken()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        AuthUserView user = authApplicationService.me(UUID.fromString(authentication.getName()));
        return new AuthUserResponse(user.userId(), user.email(), user.displayName());
    }

    private AuthTokensResponse response(AuthTokens t) {
        return new AuthTokensResponse(
                t.accessToken(), t.refreshToken(), t.tokenType(), t.expiresInSeconds());
    }
}
