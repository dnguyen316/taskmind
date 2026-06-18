package com.taskmind.ai.config.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "taskmind.logging.request")
public record RequestLoggingProperties(
        String correlationHeader, String mdcKey, boolean includeProblemDetails) {
    public RequestLoggingProperties {
        if (correlationHeader == null || correlationHeader.isBlank()) {
            correlationHeader = RequestCorrelation.HEADER_NAME;
        }
        if (mdcKey == null || mdcKey.isBlank()) {
            mdcKey = RequestCorrelation.MDC_KEY;
        }
    }
}
