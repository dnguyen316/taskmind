package com.taskmind.backend.auth.application;

import com.taskmind.backend.auth.domain.AuthException;
import com.taskmind.backend.auth.domain.AuthFailureReason;
import com.taskmind.backend.auth.domain.OtpService;
import com.taskmind.backend.auth.domain.PasswordHasher;
import com.taskmind.backend.auth.domain.TokenService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InMemoryAuthApplicationService implements AuthApplicationService {

    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    @SuppressWarnings("unused")
    private final OtpService otpService;
    private final Map<String, StoredUser> usersByEmail = new ConcurrentHashMap<>();

    public InMemoryAuthApplicationService() {
        this.passwordHasher = new Base64PasswordHasher();
        this.tokenService = new InMemoryTokenService();
        this.otpService = new NoopOtpService();
    }

    @Override
    public AuthTokens signupEmail(SignupEmailCommand command) {
        var normalizedEmail = normalizeEmail(command.email());
        if (usersByEmail.containsKey(normalizedEmail)) {
            throw new AuthException(AuthFailureReason.INVALID_CREDENTIALS, "Unable to process authentication request.");
        }

        var user = new StoredUser(UUID.randomUUID(), normalizedEmail, command.displayName(), passwordHasher.hash(command.password()));
        usersByEmail.put(normalizedEmail, user);
        return tokenService.issue(user.id(), user.email());
    }

    @Override
    public AuthTokens login(LoginCommand command) {
        var normalizedEmail = normalizeEmail(command.email());
        var user = usersByEmail.get(normalizedEmail);

        if (user == null || !passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new AuthException(AuthFailureReason.INVALID_CREDENTIALS, "Unable to process authentication request.");
        }

        return tokenService.issue(user.id(), user.email());
    }

    @Override
    public AuthTokens refresh(RefreshTokenCommand command) {
        return tokenService.rotateRefreshToken(command.refreshToken());
    }

    @Override
    public void logout(LogoutCommand command) {
        tokenService.revokeRefreshToken(command.refreshToken());
    }

    @Override
    public AuthUserView me(String authorizationHeader) {
        var user = ((InMemoryTokenService) tokenService).userFromBearer(authorizationHeader)
            .orElseThrow(() -> new AuthException(AuthFailureReason.TOKEN_INVALID, "Unable to process authentication request."));
        return new AuthUserView(user.id(), user.email(), user.displayName());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private record StoredUser(UUID id, String email, String displayName, String passwordHash) {
    }

    private static class Base64PasswordHasher implements PasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return Base64.getEncoder().encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean matches(String rawPassword, String hashedPassword) {
            return hash(rawPassword).equals(hashedPassword);
        }
    }

    private class InMemoryTokenService implements TokenService {
        private final Map<String, StoredUser> byAccessToken = new ConcurrentHashMap<>();
        private final Map<String, StoredUser> byRefreshToken = new ConcurrentHashMap<>();

        @Override
        public AuthTokens issue(UUID userId, String email) {
            var user = usersByEmail.get(email);
            var accessToken = "access-" + UUID.randomUUID();
            var refreshToken = "refresh-" + UUID.randomUUID();
            byAccessToken.put(accessToken, user);
            byRefreshToken.put(refreshToken, user);
            return new AuthTokens(accessToken, refreshToken, "Bearer", 3600);
        }

        @Override
        public AuthTokens rotateRefreshToken(String refreshToken) {
            var user = byRefreshToken.remove(refreshToken);
            if (user == null) {
                throw new AuthException(AuthFailureReason.TOKEN_INVALID, "Unable to process authentication request.");
            }
            return issue(user.id(), user.email());
        }

        @Override
        public void revokeRefreshToken(String refreshToken) {
            byRefreshToken.remove(refreshToken);
        }

        java.util.Optional<StoredUser> userFromBearer(String authorizationHeader) {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.ofNullable(byAccessToken.get(authorizationHeader.substring("Bearer ".length()).trim()));
        }
    }

    private static class NoopOtpService implements OtpService {
        @Override
        public void dispatchOtp(String channel, String destination) {
        }

        @Override
        public boolean verifyOtp(String destination, String otp) {
            return false;
        }
    }
}
