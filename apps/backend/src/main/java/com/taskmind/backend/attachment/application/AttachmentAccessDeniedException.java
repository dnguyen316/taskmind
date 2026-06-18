package com.taskmind.backend.attachment.application;

public class AttachmentAccessDeniedException extends RuntimeException {
    public AttachmentAccessDeniedException(String message) {
        super(message);
    }
}
