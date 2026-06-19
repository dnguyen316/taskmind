package com.taskmind.backend.integration.infrastructure;

import org.springframework.http.HttpStatusCode;

public class ProviderClientException extends RuntimeException {
    private final HttpStatusCode statusCode;
    private final String errorCode;
    private final boolean retrySafe;

    public ProviderClientException(HttpStatusCode statusCode, String errorCode, String message, boolean retrySafe) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.retrySafe = retrySafe;
    }

    public HttpStatusCode statusCode() {
        return statusCode;
    }

    public String errorCode() {
        return errorCode;
    }

    public boolean retrySafe() {
        return retrySafe;
    }
}
