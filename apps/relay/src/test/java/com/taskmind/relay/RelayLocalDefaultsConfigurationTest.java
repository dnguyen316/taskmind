package com.taskmind.relay;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class RelayLocalDefaultsConfigurationTest {
    private static final String SHARED_RELAY_SERVICE_TOKEN_DEFAULT = "local-relay-service-token";

    @Test
    void coreAndRelayLocalServiceTokenDefaultsStayAligned() throws IOException {
        Properties relayProperties = loadRelayProperties();
        Properties backendProperties = loadBackendProperties();

        assertThat(defaultValue(relayProperties.getProperty("taskmind.relay.service-token")))
                .isEqualTo(SHARED_RELAY_SERVICE_TOKEN_DEFAULT);
        assertThat(defaultValue(backendProperties.getProperty("taskmind.relay.client.service-token")))
                .isEqualTo(SHARED_RELAY_SERVICE_TOKEN_DEFAULT);
    }

    private Properties loadRelayProperties() throws IOException {
        Properties properties = new Properties();
        try (var inputStream = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(inputStream);
        }
        return properties;
    }

    private Properties loadBackendProperties() throws IOException {
        Properties properties = new Properties();
        try (var inputStream =
                Files.newInputStream(
                        Path.of("../backend/src/main/resources/application.properties"))) {
            properties.load(inputStream);
        }
        return properties;
    }

    private String defaultValue(String placeholder) {
        int delimiter = placeholder.lastIndexOf(':');
        assertThat(delimiter).as("placeholder %s declares a default", placeholder).isPositive();
        assertThat(placeholder).endsWith("}");
        return placeholder.substring(delimiter + 1, placeholder.length() - 1);
    }
}
