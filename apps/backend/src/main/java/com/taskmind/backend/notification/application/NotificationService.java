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

    public NotificationService(
            NotificationRepository n,
            NotificationPreferenceRepository p,
            NotificationSseHub h,
            SlackNotifier s) {
        notifications = n;
        preferences = p;
        hub = h;
        slack = s;
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
            try {
                slack.send(saved, pref);
                notifications.recordDelivery(
                        new DeliveryAttempt(
                                UUID.randomUUID(),
                                saved.id(),
                                userId,
                                NotificationChannel.SLACK,
                                DeliveryStatus.SENT,
                                null,
                                Instant.now()));
            } catch (RuntimeException ex) {
                notifications.recordDelivery(
                        new DeliveryAttempt(
                                UUID.randomUUID(),
                                saved.id(),
                                userId,
                                NotificationChannel.SLACK,
                                DeliveryStatus.FAILED,
                                ex.getMessage(),
                                Instant.now()));
            }
        }
        return saved;
    }
}
