package com.taskmind.backend.integration.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class TokenCipher {
    private static final String CURRENT_PREFIX = "enc:v2:gcm:";
    private static final String LEGACY_PREFIX = "enc:";
    private static final String LOCAL_DEVELOPMENT_KEY = "local-development-token-key";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final byte[] legacyKey;
    private final String keyVersion;
    private final SecureRandom secureRandom;

    @Autowired
    public TokenCipher(Environment environment) {
        this(environment, new SecureRandom());
    }

    TokenCipher(Environment environment, SecureRandom secureRandom) {
        String configuredKey = environment.getProperty("taskmind.integrations.token-key");
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean localOrTest = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("local") || profile.equals("test"));
        if (isBlank(configuredKey)) {
            if (prod || !localOrTest) {
                throw new IllegalStateException(
                        "taskmind.integrations.token-key must be configured for non-local profiles");
            }
            configuredKey = LOCAL_DEVELOPMENT_KEY;
        }
        this.key = new SecretKeySpec(sha256(configuredKey), "AES");
        this.legacyKey = configuredKey.getBytes(StandardCharsets.UTF_8);
        this.keyVersion = environment.getProperty("taskmind.integrations.token-key-version", "local-v1");
        this.secureRandom = secureRandom;
    }

    public String encrypt(String token) {
        if (token == null || token.isBlank()) return null;
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        byte[] ciphertext = encryptAesGcm(token.getBytes(StandardCharsets.UTF_8), nonce);
        return CURRENT_PREFIX
                + keyVersion
                + ":"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(nonce)
                + ":"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || !encrypted.startsWith(LEGACY_PREFIX)) return encrypted;
        if (encrypted.startsWith(CURRENT_PREFIX)) return decryptCurrent(encrypted);
        return decryptLegacy(encrypted);
    }

    private String decryptCurrent(String encrypted) {
        String[] parts = encrypted.split(":", 6);
        if (parts.length != 6 || parts[3].isBlank()) {
            throw new IllegalArgumentException("Invalid integration token ciphertext metadata");
        }
        byte[] nonce = Base64.getUrlDecoder().decode(parts[4]);
        byte[] ciphertext = Base64.getUrlDecoder().decode(parts[5]);
        if (nonce.length != NONCE_BYTES) {
            throw new IllegalArgumentException("Invalid integration token nonce");
        }
        return new String(decryptAesGcm(ciphertext, nonce), StandardCharsets.UTF_8);
    }

    private String decryptLegacy(String encrypted) {
        byte[] input = Base64.getDecoder().decode(encrypted.substring(LEGACY_PREFIX.length()));
        byte[] out = new byte[input.length];
        for (int i = 0; i < input.length; i++) out[i] = (byte) (input[i] ^ legacyKey[i % legacyKey.length]);
        return new String(out, StandardCharsets.UTF_8);
    }

    private byte[] encryptAesGcm(byte[] plaintext, byte[] nonce) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to encrypt integration token", ex);
        }
    }

    private byte[] decryptAesGcm(byte[] ciphertext, byte[] nonce) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException ex) {
            throw new IllegalArgumentException("Unable to decrypt integration token", ex);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
