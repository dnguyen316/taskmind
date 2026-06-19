package com.taskmind.backend.notification.application;

import com.taskmind.backend.common.SlackNotifier;
import com.taskmind.backend.notification.domain.model.*;
import com.taskmind.backend.notification.domain.repository.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final NotificationSseHub hub;
    private final SlackNotifier slack;
    private final NotificationDeliveryCoordinator delivery;

    public NotificationService(
            NotificationRepository n,
            NotificationPreferenceRepository p,
            NotificationSseHub h,
            SlackNotifier s,
            NotificationDeliveryCoordinator d) {
        notifications = n;
        preferences = p;
        hub = h;
        slack = s;
        delivery = d;
    }

    @Transactional
    public Notification notify(
            UUID userId,
            NotificationType type,
            String title,
            String body,
            UUID taskId,
            String actionUrl) {
        Instant now = Instant.now();
        NotificationPreference pref =
                preferences
                        .findByUserId(userId)
                        .orElseGet(
                                () ->
                                        preferences.save(
                                                NotificationPreference.defaults(userId, now)));
        Notification saved = null;
        if (pref.inAppEnabled()) {
            saved =
                    notifications.save(
                            new Notification(
                                    UUID.randomUUID(),
                                    null,
                                    userId,
                                    type,
                                    title,
                                    body,
                                    taskId,
                                    actionUrl,
                                    false,
                                    null,
                                    now));
            hub.publish(saved);
        }
        if (pref.slackEnabled() && saved != null) {
            Instant attemptedAt = Instant.now();
            if (delivery.shouldAttempt(saved, NotificationChannel.SLACK, attemptedAt)) {
                try {
                    slack.send(saved, pref);
                    delivery.recordSuccess(saved, NotificationChannel.SLACK, Instant.now());
                } catch (RuntimeException ex) {
                    delivery.recordFailure(saved, NotificationChannel.SLACK, ex, Instant.now());
                }
            }
        }
        return saved;
    }
}
