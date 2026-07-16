package com.taskmind.backend.notification.application;

import com.taskmind.backend.common.EmailSender;
import com.taskmind.backend.notification.domain.model.*;
import com.taskmind.backend.notification.domain.repository.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationDigestJob {
    private final JdbcTemplate jdbc;
    private final NotificationRepository notifications;
    private final NotificationPreferenceRepository preferences;
    private final EmailSender email;
    private final NotificationDeliveryCoordinator delivery;

    public NotificationDigestJob(
            JdbcTemplate jdbcTemplate,
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            EmailSender emailSender,
            NotificationDeliveryCoordinator deliveryCoordinator) {
        this.jdbc = jdbcTemplate;
        this.notifications = notificationRepository;
        this.preferences = preferenceRepository;
        this.email = emailSender;
        this.delivery = deliveryCoordinator;
    }

    @Scheduled(cron = "${taskmind.notifications.digest.cron:0 0 * * * *}")
    @SchedulerLock(name = "notificationDigestJob", lockAtMostFor = "PT15M", lockAtLeastFor = "PT5S")
    public void run() {
        Instant before = Instant.now().minus(1, ChronoUnit.HOURS);
        List<java.util.UUID> users =
                jdbc.query(
                        "select distinct recipient_user_id from notifications where read_at is null and created_at <= ?",
                        (rs, i) -> (java.util.UUID) rs.getObject(1),
                        java.sql.Timestamp.from(before));
        for (java.util.UUID userId : users) {
            NotificationPreference pref =
                    preferences
                            .findByUserId(userId)
                            .orElse(NotificationPreference.defaults(userId, Instant.now()));
            if (!pref.emailDigestEnabled()) continue;
            List<Notification> unread =
                    notifications.unreadOlderThan(userId, before).stream()
                            .filter(
                                    notification ->
                                            delivery.shouldAttempt(
                                                    notification,
                                                    NotificationChannel.EMAIL_DIGEST,
                                                    Instant.now()))
                            .toList();
            if (!unread.isEmpty()) {
                try {
                    email.sendDigest(pref, unread);
                    Instant sentAt = Instant.now();
                    unread.forEach(
                            notification ->
                                    delivery.recordSuccess(
                                            notification, NotificationChannel.EMAIL_DIGEST, sentAt));
                } catch (RuntimeException ex) {
                    Instant failedAt = Instant.now();
                    unread.forEach(
                            notification ->
                                    delivery.recordFailure(
                                            notification,
                                            NotificationChannel.EMAIL_DIGEST,
                                            ex,
                                            failedAt));
                }
            }
        }
    }
}
