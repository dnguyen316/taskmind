package com.taskmind.backend.auth.domain;

import com.taskmind.backend.auth.application.AuthTokens;
import java.util.UUID;

public interface TokenService {
    AuthTokens issue(UUID userId, String email);

    AuthTokens rotateRefreshToken(String refreshToken);

    void revokeRefreshToken(String refreshToken);
}
