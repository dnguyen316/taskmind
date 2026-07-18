package com.taskmind.backend.attachment.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.io.ClassPathResource;

class AttachmentStoragePropertiesTest {
    @Test
    void defaultsS3RegionToSydney() {
        AttachmentStorageProperties properties = new AttachmentStorageProperties();

        assertThat(properties.getS3Region()).isEqualTo("ap-southeast-2");
    }

    @Test
    void bindsExplicitS3RegionOverride() {
        AttachmentStorageProperties properties =
                new Binder(
                                new MapConfigurationPropertySource(
                                        java.util.Map.of(
                                                "taskmind.attachments.s3-region", "us-east-1")))
                        .bind("taskmind.attachments", AttachmentStorageProperties.class)
                        .get();

        assertThat(properties.getS3Region()).isEqualTo("us-east-1");
    }

    @Test
    void applicationPropertiesKeepsEnvironmentOverrideWithSydneyFallback() throws IOException {
        Properties properties = applicationProperties();

        assertThat(properties.getProperty("taskmind.attachments.s3-region"))
                .isEqualTo("${TASKMIND_ATTACHMENTS_S3_REGION:ap-southeast-2}");
    }

    private Properties applicationProperties() throws IOException {
        Properties properties = new Properties();
        try (var inputStream = new ClassPathResource("application.properties").getInputStream()) {
            properties.load(inputStream);
        }
        return properties;
    }
}
