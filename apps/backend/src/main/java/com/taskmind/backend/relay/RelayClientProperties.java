package com.taskmind.backend.relay;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "taskmind.relay.client")
public record RelayClientProperties(@NotBlank String baseUrl, @NotBlank String serviceToken) {}
