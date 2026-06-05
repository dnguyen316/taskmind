package com.taskmind.backend.auth.infrastructure.security;

import com.taskmind.backend.auth.application.AuthTokens;
import com.taskmind.backend.auth.domain.TokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService implements TokenService {
    private final JwtEncoder encoder;
    private final Duration accessTtl;
    private final SecureRandom random = new SecureRandom();

    public JwtTokenService(JwtEncoder encoder, @Value("${taskmind.auth.jwt.access-ttl:PT1H}") Duration accessTtl) {
        this.encoder = encoder; this.accessTtl = accessTtl;
    }

    @Override
    public AuthTokens issue(UUID userId, String email, Set<String> roles) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("taskmind-core").issuedAt(now).expiresAt(now.plus(accessTtl))
                .subject(userId.toString()).claim("email", email).claim("roles", roles).build();
        var access = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        var refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new AuthTokens(access, refresh, "Bearer", accessTtl.toSeconds());
    }

    @Override
    public String hashRefreshToken(String refreshToken) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
