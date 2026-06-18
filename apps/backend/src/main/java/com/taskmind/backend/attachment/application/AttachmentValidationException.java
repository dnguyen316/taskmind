package com.taskmind.backend.attachment.application;

public class AttachmentValidationException extends RuntimeException {
    private final boolean sizeLimitViolation;

    public AttachmentValidationException(String message) {
        this(message, false);
    }

    public AttachmentValidationException(String message, boolean sizeLimitViolation) {
        super(message);
        this.sizeLimitViolation = sizeLimitViolation;
    }

    public boolean isSizeLimitViolation() {
        return sizeLimitViolation;
    }
}
