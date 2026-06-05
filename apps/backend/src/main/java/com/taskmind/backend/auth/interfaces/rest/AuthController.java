package com.taskmind.backend.auth.interfaces.rest;

import com.taskmind.backend.auth.application.AuthApplicationService;
import com.taskmind.backend.auth.application.LoginCommand;
import com.taskmind.backend.auth.application.LogoutCommand;
import com.taskmind.backend.auth.application.RefreshTokenCommand;
import com.taskmind.backend.auth.application.SignupEmailCommand;
import com.taskmind.backend.auth.interfaces.rest.dto.AuthTokensResponse;
import com.taskmind.backend.auth.interfaces.rest.dto.AuthUserResponse;
import com.taskmind.backend.auth.interfaces.rest.dto.LoginRequest;
import com.taskmind.backend.auth.interfaces.rest.dto.LogoutRequest;
import com.taskmind.backend.auth.interfaces.rest.dto.RefreshTokenRequest;
import com.taskmind.backend.auth.interfaces.rest.dto.SignupEmailRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@Validated
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/signup/email")
    public ResponseEntity<AuthTokensResponse> signupEmail(
            @Valid @RequestBody SignupEmailRequest request) {
        var tokens =
                authApplicationService.signupEmail(
                        new SignupEmailCommand(
                                request.email(), request.password(), request.displayName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toTokensResponse(tokens));
    }

    @PostMapping("/login")
    public AuthTokensResponse login(@Valid @RequestBody LoginRequest request) {
        var tokens =
                authApplicationService.login(new LoginCommand(request.email(), request.password()));
        return toTokensResponse(tokens);
    }

    @PostMapping("/token/refresh")
    public AuthTokensResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var tokens =
                authApplicationService.refresh(new RefreshTokenCommand(request.refreshToken()));
        return toTokensResponse(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authApplicationService.logout(new LogoutCommand(request.refreshToken()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthUserResponse me(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        var user = authApplicationService.me(authorizationHeader);
        return new AuthUserResponse(user.userId(), user.email(), user.displayName());
    }

    private AuthTokensResponse toTokensResponse(
            com.taskmind.backend.auth.application.AuthTokens tokens) {
        return new AuthTokensResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.expiresInSeconds());
    }
}
