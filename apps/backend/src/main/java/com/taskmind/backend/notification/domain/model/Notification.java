package com.taskmind.backend.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Notification(
        UUID id,
        Long version,
        UUID recipientUserId,
        NotificationType type,
        String title,
        String body,
        UUID taskId,
        String actionUrl,
        boolean read,
        Instant readAt,
        Instant createdAt) {
    public Notification {
        if (recipientUserId == null) throw new IllegalArgumentException("Recipient is required");
        if (type == null) throw new IllegalArgumentException("Notification type is required");
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Notification title is required");
    }

    public Notification markRead(Instant now) {
        return read
                ? this
                : new Notification(
                        id,
                        version,
                        recipientUserId,
                        type,
                        title,
                        body,
                        taskId,
                        actionUrl,
                        true,
                        now,
                        createdAt);
    }
}
