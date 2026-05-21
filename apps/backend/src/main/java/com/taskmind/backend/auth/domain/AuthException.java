package com.taskmind.backend.auth.domain;

public class AuthException extends RuntimeException {

    private final AuthFailureReason reason;

    public AuthException(AuthFailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public AuthFailureReason reason() {
        return reason;
    }
}
