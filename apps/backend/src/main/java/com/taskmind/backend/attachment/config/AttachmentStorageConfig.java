package com.taskmind.backend.attachment.config;

import com.taskmind.backend.attachment.domain.repository.ObjectStoragePort;
import com.taskmind.backend.attachment.infrastructure.storage.FilesystemObjectStorageAdapter;
import com.taskmind.backend.attachment.infrastructure.storage.S3ObjectStorageAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AttachmentStorageProperties.class)
public class AttachmentStorageConfig {
    @Bean
    @ConditionalOnMissingBean(ObjectStoragePort.class)
    ObjectStoragePort objectStoragePort(AttachmentStorageProperties properties) {
        if ("s3".equalsIgnoreCase(properties.getAdapter())) {
            return new S3ObjectStorageAdapter(properties);
        }
        return new FilesystemObjectStorageAdapter(properties.getFilesystemRoot());
    }
}
