package com.taskmind.backend.ai;

import org.springframework.http.HttpStatusCode;

public class NovaClientException extends RuntimeException {
    private final HttpStatusCode statusCode;
    private final String errorCode;

    public NovaClientException(HttpStatusCode statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public HttpStatusCode statusCode() {
        return statusCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
