package com.taskmind.backend.auth.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.taskmind.backend.auth.domain.PasswordHasher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class AuthSecurityConfiguration {

    @Bean
    PasswordHasher passwordHasher() {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new BCryptPasswordEncoder(12);
        return new PasswordHasher() {
            @Override
            public String hash(String rawPassword) {
                return encoder.encode(rawPassword);
            }

            @Override
            public boolean matches(String rawPassword, String hashedPassword) {
                return encoder.matches(rawPassword, hashedPassword);
            }
        };
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${taskmind.auth.jwt.secret}") String secret) {
        if (secret.length() < 32) {
            throw new IllegalStateException(
                    "taskmind.auth.jwt.secret must contain at least 32 characters");
        }
        return new SecretKeySpec(
                secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder jwtDecoder(
            SecretKey key,
            @Value("${taskmind.auth.jwt.issuer}") String issuer,
            @Value("${taskmind.auth.jwt.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).build();
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithValidators(
                        JwtValidators.createDefaultWithIssuer(issuer), new AudienceValidator(audience)));
        return decoder;
    }

    private record AudienceValidator(String audience) implements OAuth2TokenValidator<Jwt> {
        private static final OAuth2Error MISSING_OR_INVALID_AUDIENCE =
                new OAuth2Error(
                        "invalid_token",
                        "The token is missing the required TaskMind Core audience.",
                        null);

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            if (token.getAudience() != null && token.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(MISSING_OR_INVALID_AUDIENCE);
        }
    }
}
