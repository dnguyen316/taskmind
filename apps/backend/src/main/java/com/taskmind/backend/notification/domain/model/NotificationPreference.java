package com.taskmind.backend.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationPreference(
        UUID userId,
        Long version,
        boolean inAppEnabled,
        boolean emailDigestEnabled,
        boolean slackEnabled,
        String slackWebhookUrl,
        String slackChannel,
        Instant createdAt,
        Instant updatedAt) {
    public static NotificationPreference defaults(UUID userId, Instant now) {
        return new NotificationPreference(userId, null, true, true, false, null, null, now, now);
    }

    public boolean channelEnabled(NotificationChannel channel) {
        return switch (channel) {
            case IN_APP -> inAppEnabled;
            case EMAIL_DIGEST -> emailDigestEnabled;
            case SLACK -> slackEnabled;
        };
    }
}
