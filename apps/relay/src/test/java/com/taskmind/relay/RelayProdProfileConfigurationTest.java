package com.taskmind.relay;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class RelayProdProfileConfigurationTest {

    @Test
    void prodProfileRequiresRelayDeploymentValuesWithoutLocalFallbacks() throws IOException {
        Properties properties = prodProperties();

        assertThat(properties.getProperty("spring.data.redis.host"))
                .isEqualTo("${TASKMIND_REDIS_HOST}");
        assertThat(properties.getProperty("spring.data.redis.port"))
                .isEqualTo("${TASKMIND_REDIS_PORT}");
        assertThat(properties.getProperty("spring.data.redis.password"))
                .isEqualTo("${TASKMIND_REDIS_PASSWORD}");
        assertThat(properties.getProperty("spring.elasticsearch.uris"))
                .isEqualTo("${TASKMIND_OPENSEARCH_ENDPOINT}");
        assertThat(properties.getProperty("taskmind.relay.service-token"))
                .isEqualTo("${TASKMIND_RELAY_SERVICE_TOKEN}");
        assertThat(properties.getProperty("taskmind.relay.stream-key"))
                .isEqualTo("${TASKMIND_RELAY_STREAM_KEY}");
        assertThat(properties.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info");
        assertThat(properties.getProperty("logging.level.com.taskmind.relay")).isEqualTo("INFO");
    }

    @Test
    void prodProfileDoesNotUseDevelopmentDefaultsForRequiredValues() throws IOException {
        Properties properties = prodProperties();

        assertThat(properties.getProperty("taskmind.relay.service-token"))
                .doesNotContain("development-service-token");
        assertThat(properties.getProperty("spring.elasticsearch.uris"))
                .doesNotContain("localhost");
        assertThat(properties.getProperty("spring.data.redis.host")).doesNotContain(":");
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
