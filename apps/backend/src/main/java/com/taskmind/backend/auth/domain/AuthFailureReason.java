package com.taskmind.backend.auth.domain;

public enum AuthFailureReason {
    INVALID_CREDENTIALS,
    TOKEN_INVALID,
    TOKEN_EXPIRED,
    USER_NOT_FOUND
}
