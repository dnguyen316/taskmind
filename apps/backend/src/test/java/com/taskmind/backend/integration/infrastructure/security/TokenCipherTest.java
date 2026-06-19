package com.taskmind.backend.integration.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class TokenCipherTest {
    @Test
    void encryptedTokensIncludeAuthenticatedCiphertextMetadataAndRoundTrip() {
        TokenCipher cipher = cipher("test-profile-key-one", "2026-06");

        String encrypted = cipher.encrypt("ghp_secret");

        assertThat(encrypted).startsWith("enc:v2:gcm:2026-06:");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("ghp_secret");
    }

    @Test
    void tokensCannotDecryptWithWrongKey() {
        String encrypted = cipher("test-profile-key-one", "2026-06").encrypt("jira-secret");

        assertThatThrownBy(() -> cipher("different-profile-key", "2026-06").decrypt(encrypted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to decrypt integration token");
    }

    @Test
    void tamperedCiphertextIsRejected() {
        String encrypted = cipher("test-profile-key-one", "2026-06").encrypt("jira-secret");
        String tampered = encrypted.substring(0, encrypted.length() - 1)
                + (encrypted.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> cipher("test-profile-key-one", "2026-06").decrypt(tampered))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unable to decrypt integration token");
    }

    @Test
    void blankAndNullTokensAreHandledAsAbsentSecrets() {
        TokenCipher cipher = cipher("test-profile-key-one", "2026-06");

        assertThat(cipher.encrypt(null)).isNull();
        assertThat(cipher.encrypt("  ")).isNull();
        assertThat(cipher.decrypt(null)).isNull();
        assertThat(cipher.decrypt("plain-token")).isEqualTo("plain-token");
    }

    @Test
    void prodProfileFailsWhenTokenKeyIsMissing() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new TokenCipher(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("taskmind.integrations.token-key must be configured");
    }

    @Test
    void legacyEncValuesRemainDecryptableWithConfiguredKey() {
        String key = "legacy-key";
        byte[] input = "old-secret".getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[input.length];
        for (int i = 0; i < input.length; i++) out[i] = (byte) (input[i] ^ keyBytes[i % keyBytes.length]);
        String legacy = "enc:" + Base64.getEncoder().encodeToString(out);

        assertThat(cipher(key, "legacy").decrypt(legacy)).isEqualTo("old-secret");
    }

    private TokenCipher cipher(String key, String version) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        environment.setProperty("taskmind.integrations.token-key", key);
        environment.setProperty("taskmind.integrations.token-key-version", version);
        return new TokenCipher(environment);
    }
}
