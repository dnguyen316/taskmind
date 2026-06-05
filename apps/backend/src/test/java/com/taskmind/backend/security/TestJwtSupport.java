package com.taskmind.backend.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class TestJwtSupport {

    private static final RSAKey SIGNING_KEY = generateSigningKey();
    private static final JwtEncoder JWT_ENCODER = new NimbusJwtEncoder(
        new ImmutableJWKSet<SecurityContext>(new JWKSet(SIGNING_KEY))
    );

    private TestJwtSupport() {
    }

    public static RequestPostProcessor jwt(String subject, String... roles) {
        return request -> {
            request.addHeader("Authorization", "Bearer " + token(subject, roles));
            return request;
        };
    }

    private static String token(String subject, String... roles) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
            .subject(subject)
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300));
        if (roles.length > 0) {
            claims.claim("roles", List.copyOf(Arrays.asList(roles)));
        }
        return JWT_ENCODER.encode(JwtEncoderParameters.from(
            JwsHeader.with(SignatureAlgorithm.RS256).build(),
            claims.build()
        )).getTokenValue();
    }

    private static RSAKey generateSigningKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("taskmind-test-key").generate();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not generate test JWT signing key", exception);
        }
    }

    @TestConfiguration
    public static class Config {

        @Bean
        JwtDecoder jwtDecoder() throws Exception {
            return NimbusJwtDecoder.withPublicKey(SIGNING_KEY.toRSAPublicKey()).build();
        }
    }
}
