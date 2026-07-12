package com.taskmind.backend.notification.application;

import com.taskmind.backend.common.EmailSender;
import com.taskmind.backend.common.SlackNotifier;
import com.taskmind.backend.notification.domain.model.*;
import com.taskmind.backend.notification.domain.repository.*;
import java.time.Instant;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationDeliveryWorker {
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final SlackNotifier slack;
    private final EmailSender email;
    private final NotificationDeliveryCoordinator delivery;
    private final int batchSize;

    public NotificationDeliveryWorker(
            NotificationRepository notifications,
            NotificationPreferenceRepository preferences,
            SlackNotifier slack,
            EmailSender email,
            NotificationDeliveryCoordinator delivery,
            @Value("${taskmind.notifications.delivery.batch-size:50}") int batchSize) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.slack = slack;
        this.email = email;
        this.delivery = delivery;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${taskmind.notifications.delivery.fixed-delay:PT30S}")
    @SchedulerLock(name = "notificationDeliveryWorker", lockAtMostFor = "PT5M", lockAtLeastFor = "PT1S")
    public void run() {
        processBatch(Instant.now(), batchSize);
    }

    @Transactional
    public int processBatch(Instant now, int limit) {
        List<DeliveryAttempt> attempts = notifications.claimPendingDeliveries(now, limit);
        attempts.forEach(attempt -> process(attempt, now));
        return attempts.size();
    }

    private void process(DeliveryAttempt attempt, Instant now) {
        notifications.findById(attempt.notificationId())
                .ifPresent(notification -> processExistingNotification(attempt, notification, now));
    }

    private void processExistingNotification(
            DeliveryAttempt attempt, Notification notification, Instant now) {
        if (!delivery.shouldAttempt(attempt, now)) {
            return;
        }
        NotificationPreference preference =
                preferences
                        .findByUserId(attempt.userId())
                        .orElse(NotificationPreference.defaults(attempt.userId(), now));
        try {
            if (attempt.channel() == NotificationChannel.SLACK) {
                slack.send(notification, preference);
            } else if (attempt.channel() == NotificationChannel.EMAIL_DIGEST) {
                email.sendDigest(preference, List.of(notification));
            } else {
                throw new IllegalArgumentException("Unsupported delivery channel " + attempt.channel());
            }
            delivery.recordSuccess(attempt, now);
        } catch (RuntimeException ex) {
            delivery.recordFailure(attempt, ex, now);
        }
    }
}
