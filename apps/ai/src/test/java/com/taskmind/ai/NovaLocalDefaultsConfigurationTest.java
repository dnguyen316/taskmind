package com.taskmind.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

class NovaLocalDefaultsConfigurationTest {
    private static final String SHARED_NOVA_SERVICE_TOKEN_DEFAULT =
            "development-only-nova-service-token";

    @Test
    void novaUsesCanonicalNovaServiceTokenWithDeprecatedAiAliasAndMatchingLocalDefault() {
        Properties properties = applicationYamlProperties();

        assertThat(properties.getProperty("taskmind.ai.service-token"))
                .isEqualTo(
                        "${TASKMIND_NOVA_SERVICE_TOKEN:${TASKMIND_AI_SERVICE_TOKEN:"
                                + SHARED_NOVA_SERVICE_TOKEN_DEFAULT
                                + "}}");
        assertThat(properties.getProperty("spring.datasource.url"))
                .isEqualTo(
                        "${TASKMIND_AI_DB_URL:${TASKMIND_DB_URL:jdbc:postgresql://localhost:5432/taskmind}}");
        assertThat(properties.getProperty("spring.datasource.username"))
                .isEqualTo("${TASKMIND_AI_DB_USERNAME:${TASKMIND_DB_USERNAME:taskmind}}");
        assertThat(properties.getProperty("spring.datasource.password"))
                .isEqualTo("${TASKMIND_AI_DB_PASSWORD:${TASKMIND_DB_PASSWORD:taskmind}}");
    }

    private Properties applicationYamlProperties() {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-local.yml"));
        Properties properties = factory.getObject();
        assertThat(properties).isNotNull();
        return properties;
    }
}
