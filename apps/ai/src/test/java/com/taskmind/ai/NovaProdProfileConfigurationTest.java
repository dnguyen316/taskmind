package com.taskmind.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class NovaProdProfileConfigurationTest {

    @Test
    void prodProfileRequiresNovaDeploymentValuesWithoutLocalFallbacks() throws IOException {
        Properties properties = prodProperties();

        assertThat(properties.getProperty("taskmind.ai.provider.default"))
                .isEqualTo("${TASKMIND_AI_PROVIDER_DEFAULT}");
        assertThat(properties.getProperty("taskmind.ai.provider.openai.api-key"))
                .isEqualTo("${OPENAI_API_KEY}");
        assertThat(properties.getProperty("taskmind.ai.provider.anthropic.api-key"))
                .isEqualTo("${ANTHROPIC_API_KEY}");
        assertThat(properties.getProperty("spring.datasource.url"))
                .isEqualTo("${TASKMIND_AI_DB_URL}");
        assertThat(properties.getProperty("spring.flyway.enabled")).isEqualTo("true");
        assertThat(properties.getProperty("spring.flyway.schemas")).isEqualTo("ai");
        assertThat(properties.getProperty("taskmind.ai.service-token"))
                .isEqualTo("${TASKMIND_NOVA_SERVICE_TOKEN:${TASKMIND_AI_SERVICE_TOKEN}}");
        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,prometheus");
        assertThat(properties.getProperty("logging.level.com.taskmind.ai")).isEqualTo("INFO");
    }

    @Test
    void prodProfileDoesNotUseMockOrLocalDefaultsForRequiredValues() throws IOException {
        Properties properties = prodProperties();

        assertThat(properties.getProperty("taskmind.ai.provider.default")).doesNotContain("mock");
        assertThat(properties.getProperty("taskmind.ai.service-token")).doesNotContain("local");
        assertThat(properties.getProperty("spring.datasource.url")).doesNotContain("localhost");
        assertThat(properties.getProperty("taskmind.core.base-url")).doesNotContain("localhost");
        assertThat(properties.getProperty("taskmind.relay.base-url")).doesNotContain("localhost");
    }

    private Properties prodProperties() throws IOException {
        Properties properties = new Properties();
        try (var inputStream =
                new ClassPathResource("application-prod.properties").getInputStream()) {
            properties.load(inputStream);
        }
        return properties;
    }
}
