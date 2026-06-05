package com.taskmind.backend.auth.infrastructure.security;

import com.taskmind.backend.auth.domain.PasswordHasher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class AuthSecurityConfiguration {
    @Bean
    PasswordHasher passwordHasher() {
        var encoder = new BCryptPasswordEncoder(12);
        return new PasswordHasher() {
            public String hash(String rawPassword) { return encoder.encode(rawPassword); }
            public boolean matches(String rawPassword, String hashedPassword) { return encoder.matches(rawPassword, hashedPassword); }
        };
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${taskmind.auth.jwt.secret}") String secret) {
        if (secret.length() < 32) throw new IllegalStateException("taskmind.auth.jwt.secret must contain at least 32 characters");
        return new SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) { return new NimbusJwtEncoder(new ImmutableSecret<>(key)); }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) { return NimbusJwtDecoder.withSecretKey(key).build(); }
}
