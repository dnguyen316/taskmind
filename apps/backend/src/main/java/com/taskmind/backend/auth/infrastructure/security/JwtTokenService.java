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
    private final String issuer;
    private final String audience;
    private final SecureRandom random = new SecureRandom();

    public JwtTokenService(
            JwtEncoder encoder,
            @Value("${taskmind.auth.jwt.access-ttl:PT1H}") Duration accessTtl,
            @Value("${taskmind.auth.jwt.issuer}") String issuer,
            @Value("${taskmind.auth.jwt.audience}") String audience) {
        this.encoder = encoder;
        this.accessTtl = accessTtl;
        this.issuer = issuer;
        this.audience = audience;
    }

    @Override
    public AuthTokens issue(UUID userId, String email, Set<String> roles, Set<String> permissions) {
        Instant now = Instant.now();
        org.springframework.security.oauth2.jwt.JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .audience(java.util.List.of(audience))
                        .issuedAt(now)
                        .expiresAt(now.plus(accessTtl))
                        .subject(userId.toString())
                        .claim("email", email)
                        .claim("roles", roles)
                        .claim("permissions", permissions)
                        .claim("authorities", permissions)
                        .build();
        String access =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new AuthTokens(access, refresh, "Bearer", accessTtl.toSeconds());
    }

    @Override
    public String hashRefreshToken(String refreshToken) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
