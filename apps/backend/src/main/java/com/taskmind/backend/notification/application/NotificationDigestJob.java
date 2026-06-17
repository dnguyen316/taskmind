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

    public NotificationDigestJob(
            JdbcTemplate j,
            NotificationRepository n,
            NotificationPreferenceRepository p,
            EmailSender e) {
        jdbc = j;
        notifications = n;
        preferences = p;
        email = e;
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
        for (java.util.UUID u : users) {
            NotificationPreference pref =
                    preferences
                            .findByUserId(u)
                            .orElse(NotificationPreference.defaults(u, Instant.now()));
            if (!pref.emailDigestEnabled()) continue;
            List<Notification> unread = notifications.unreadOlderThan(u, before);
            if (!unread.isEmpty()) email.sendDigest(pref, unread);
        }
    }
}
