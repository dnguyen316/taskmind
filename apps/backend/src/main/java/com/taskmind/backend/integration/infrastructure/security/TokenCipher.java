package com.taskmind.backend.integration.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenCipher {
    private final byte[] key;

    public TokenCipher(@Value("${taskmind.integrations.token-key:local-development-token-key}") String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    public String encrypt(String token) {
        if (token == null || token.isBlank()) return null;
        byte[] input = token.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[input.length];
        for (int i = 0; i < input.length; i++) out[i] = (byte) (input[i] ^ key[i % key.length]);
        return "enc:" + Base64.getEncoder().encodeToString(out);
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || !encrypted.startsWith("enc:")) return encrypted;
        byte[] input = Base64.getDecoder().decode(encrypted.substring(4));
        byte[] out = new byte[input.length];
        for (int i = 0; i < input.length; i++) out[i] = (byte) (input[i] ^ key[i % key.length]);
        return new String(out, StandardCharsets.UTF_8);
    }
}
