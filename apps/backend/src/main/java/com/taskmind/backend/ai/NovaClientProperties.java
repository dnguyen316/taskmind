package com.taskmind.backend.ai;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "taskmind.nova")
public record NovaClientProperties(@NotBlank String baseUrl, @NotBlank String serviceToken) {}
