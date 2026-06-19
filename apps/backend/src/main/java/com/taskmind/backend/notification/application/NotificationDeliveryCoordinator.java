package com.taskmind.backend.notification.application;

import com.taskmind.backend.notification.domain.model.*;
import com.taskmind.backend.notification.domain.repository.NotificationRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryCoordinator {
    private final NotificationRepository notifications;
    private final Duration backoff;

    public NotificationDeliveryCoordinator(
            NotificationRepository notifications,
            @Value("${taskmind.notifications.delivery.retry-backoff:PT15M}") Duration backoff) {
        this.notifications = notifications;
        this.backoff = backoff;
    }

    public boolean shouldAttempt(Notification notification, NotificationChannel channel, Instant now) {
        return notifications
                .latestDeliveryAttempt(notification.id(), channel)
                .map(last -> shouldRetry(last, now))
                .orElse(true);
    }

    public void recordSuccess(Notification notification, NotificationChannel channel, Instant now) {
        notifications.recordDelivery(attempt(notification, channel, DeliveryStatus.SENT, null, now));
    }

    public void recordFailure(
            Notification notification, NotificationChannel channel, RuntimeException exception, Instant now) {
        notifications.recordDelivery(
                attempt(notification, channel, DeliveryStatus.FAILED, safeMessage(exception), now));
    }

    private boolean shouldRetry(DeliveryAttempt last, Instant now) {
        if (last.status() == DeliveryStatus.SENT) {
            return false;
        }
        return !last.attemptedAt().plus(backoff).isAfter(now);
    }

    private static DeliveryAttempt attempt(
            Notification notification,
            NotificationChannel channel,
            DeliveryStatus status,
            String error,
            Instant now) {
        return new DeliveryAttempt(
                UUID.randomUUID(),
                notification.id(),
                notification.recipientUserId(),
                channel,
                status,
                error,
                now);
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
