package com.taskmind.backend.auth.domain;

import com.taskmind.backend.auth.application.AuthTokens;
import java.util.Set;
import java.util.UUID;

public interface TokenService {
    AuthTokens issue(UUID userId, String email, Set<String> roles, Set<String> permissions);

    String hashRefreshToken(String refreshToken);
}
