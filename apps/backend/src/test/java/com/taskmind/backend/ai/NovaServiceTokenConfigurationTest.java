package com.taskmind.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class NovaServiceTokenConfigurationTest {
    private static final String SHARED_NOVA_SERVICE_TOKEN_DEFAULT =
            "development-only-nova-service-token";

    @Test
    void coreUsesCanonicalNovaServiceTokenWithDeprecatedAiAliasAndMatchingLocalDefault()
            throws IOException {
        Properties properties = applicationProperties();

        assertThat(properties.getProperty("taskmind.nova.service-token"))
                .isEqualTo(
                        "${TASKMIND_NOVA_SERVICE_TOKEN:${TASKMIND_AI_SERVICE_TOKEN:"
                                + SHARED_NOVA_SERVICE_TOKEN_DEFAULT
                                + "}}");
    }

    private Properties applicationProperties() throws IOException {
        Properties properties = new Properties();
        try (var inputStream = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(inputStream);
        }
        return properties;
    }
}
