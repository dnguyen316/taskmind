package com.taskmind.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class BackendProdProfileConfigurationTest {

    @Test
    void prodProfileRequiresRelayServiceTokenWithoutLocalFallback() throws IOException {
        Properties properties = prodProperties();

        assertThat(properties.getProperty("taskmind.relay.client.service-token"))
                .isEqualTo("${TASKMIND_RELAY_SERVICE_TOKEN}");
        assertThat(properties.getProperty("taskmind.relay.client.service-token"))
                .doesNotContain("local-relay-service-token");
    }

    private Properties prodProperties() throws IOException {
        Properties properties = new Properties();
        try (var inputStream = new ClassPathResource("application-prod.properties").getInputStream()) {
            properties.load(inputStream);
        }
        return properties;
    }
}
