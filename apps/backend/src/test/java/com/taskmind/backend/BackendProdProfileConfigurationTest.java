package com.taskmind.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class BackendProdProfileConfigurationTest {

    @Test
    void prodProfileRequiresServiceTokensWithoutLocalFallbacks() throws IOException {
        Properties properties = prodProperties();

        assertThat(properties.getProperty("taskmind.nova.service-token"))
                .isEqualTo("${TASKMIND_NOVA_SERVICE_TOKEN:${TASKMIND_AI_SERVICE_TOKEN}}");
        assertThat(properties.getProperty("taskmind.nova.service-token"))
                .doesNotContain("development-only-nova-service-token");
        assertThat(properties.getProperty("taskmind.relay.client.service-token"))
                .isEqualTo("${TASKMIND_RELAY_SERVICE_TOKEN}");
        assertThat(properties.getProperty("taskmind.relay.client.service-token"))
                .doesNotContain("local-relay-service-token");
        assertThat(properties.getProperty("spring.datasource.password"))
                .isEqualTo("${TASKMIND_DB_PASSWORD}");
        assertThat(properties.getProperty("taskmind.integrations.token-key"))
                .isEqualTo("${TASKMIND_INTEGRATIONS_TOKEN_KEY}");
    }

    private Properties prodProperties() throws IOException {
        Properties properties = new Properties();
        try (var inputStream = new ClassPathResource("application-prod.properties").getInputStream()) {
            properties.load(inputStream);
        }
        return properties;
    }
}
