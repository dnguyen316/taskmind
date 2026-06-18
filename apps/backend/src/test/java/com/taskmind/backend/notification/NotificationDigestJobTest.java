package com.taskmind.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskmind.backend.common.EmailSender;
import com.taskmind.backend.notification.application.NotificationDigestJob;
import com.taskmind.backend.notification.domain.model.DeliveryAttempt;
import com.taskmind.backend.notification.domain.model.Notification;
import com.taskmind.backend.notification.domain.model.NotificationPreference;
import com.taskmind.backend.notification.domain.model.NotificationType;
import com.taskmind.backend.notification.domain.model.ReminderState;
import com.taskmind.backend.notification.domain.repository.NotificationPreferenceRepository;
import com.taskmind.backend.notification.domain.repository.NotificationRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class NotificationDigestJobTest {
    private static final String DIGEST_USERS_SQL =
            "select distinct recipient_user_id from notifications where read_at is null and created_at <= ?";

    private final FakeJdbcTemplate jdbc = new FakeJdbcTemplate();
    private final FakeNotificationRepository notifications = new FakeNotificationRepository();
    private final FakeNotificationPreferenceRepository preferences =
            new FakeNotificationPreferenceRepository();
    private final FakeEmailSender email = new FakeEmailSender();
    private final NotificationDigestJob job =
            new NotificationDigestJob(jdbc, notifications, preferences, email);

    @Test
    void queriesUsersWithUnreadNotificationsOlderThanCutoff() {
        UUID userId = UUID.randomUUID();
        jdbc.digestUsers = List.of(userId);
        preferences.preference =
                Optional.of(preference(userId, true, Instant.parse("2026-06-18T10:00:00Z")));

        Instant startedAt = Instant.now();
        job.run();
        Instant finishedAt = Instant.now();

        assertThat(jdbc.queriedSql).isEqualTo(DIGEST_USERS_SQL);
        assertThat(preferences.requestedUserId).isEqualTo(userId);
        assertThat(jdbc.cutoff.toInstant())
                .isBetween(startedAt.minusSeconds(3601), finishedAt.minusSeconds(3599));
    }

    @Test
    void sendsDigestWhenEmailDigestEnabledAndUnreadNotificationsExist() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-06-18T09:00:00Z");
        NotificationPreference preference = preference(userId, true, createdAt);
        Notification unread = notification(userId, createdAt);
        jdbc.digestUsers = List.of(userId);
        preferences.preference = Optional.of(preference);
        notifications.unread = List.of(unread);

        job.run();

        assertThat(email.sentPreference).isEqualTo(preference);
        assertThat(email.sentNotifications).containsExactly(unread);
        assertThat(notifications.unreadUserId).isEqualTo(userId);
        assertThat(notifications.unreadBefore).isEqualTo(jdbc.cutoff.toInstant());
    }

    @Test
    void skipsDigestWhenEmailDigestIsDisabled() {
        UUID userId = UUID.randomUUID();
        jdbc.digestUsers = List.of(userId);
        preferences.preference =
                Optional.of(preference(userId, false, Instant.parse("2026-06-18T10:00:00Z")));

        job.run();

        assertThat(notifications.unreadCalled).isFalse();
        assertThat(email.sendCount).isZero();
    }

    @Test
    void skipsDigestWhenNoUnreadNotificationsAreReturned() {
        UUID userId = UUID.randomUUID();
        jdbc.digestUsers = List.of(userId);
        preferences.preference =
                Optional.of(preference(userId, true, Instant.parse("2026-06-18T10:00:00Z")));
        notifications.unread = List.of();

        job.run();

        assertThat(notifications.unreadCalled).isTrue();
        assertThat(email.sendCount).isZero();
    }

    @Test
    void appliesDefaultPreferencesWhenNoPreferenceRowExists() {
        UUID userId = UUID.randomUUID();
        Notification unread = notification(userId, Instant.parse("2026-06-18T09:00:00Z"));
        jdbc.digestUsers = List.of(userId);
        preferences.preference = Optional.empty();
        notifications.unread = List.of(unread);

        job.run();

        assertThat(email.sentNotifications).containsExactly(unread);
        assertThat(email.sentPreference)
                .extracting(
                        NotificationPreference::userId,
                        NotificationPreference::inAppEnabled,
                        NotificationPreference::emailDigestEnabled,
                        NotificationPreference::slackEnabled)
                .containsExactly(userId, true, true, false);
        assertThat(email.sentPreference.createdAt()).isNotNull();
        assertThat(email.sentPreference.updatedAt()).isNotNull();
    }

    private static NotificationPreference preference(
            UUID userId, boolean emailDigestEnabled, Instant createdAt) {
        return new NotificationPreference(
                userId, 7L, true, emailDigestEnabled, false, null, null, createdAt, createdAt);
    }

    private static Notification notification(UUID userId, Instant createdAt) {
        return new Notification(
                UUID.randomUUID(),
                3L,
                userId,
                NotificationType.TASK_ASSIGNED,
                "Task assigned",
                "You have a new task",
                UUID.randomUUID(),
                "/tasks/1",
                false,
                null,
                createdAt);
    }

    private static final class FakeJdbcTemplate extends JdbcTemplate {
        private List<UUID> digestUsers = List.of();
        private String queriedSql;
        private Timestamp cutoff;

        @Override
        @SuppressWarnings("unchecked")
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            queriedSql = sql;
            cutoff = (Timestamp) args[0];
            return (List<T>) digestUsers;
        }
    }

    private static final class FakeNotificationPreferenceRepository
            implements NotificationPreferenceRepository {
        private Optional<NotificationPreference> preference = Optional.empty();
        private UUID requestedUserId;

        @Override
        public Optional<NotificationPreference> findByUserId(UUID userId) {
            requestedUserId = userId;
            return preference;
        }

        @Override
        public NotificationPreference save(NotificationPreference preference) {
            this.preference = Optional.of(preference);
            return preference;
        }
    }

    private static final class FakeNotificationRepository implements NotificationRepository {
        private List<Notification> unread = List.of();
        private boolean unreadCalled;
        private UUID unreadUserId;
        private Instant unreadBefore;

        @Override
        public Notification save(Notification n) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Notification> findById(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Notification> findByRecipient(UUID userId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long unreadCount(UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int markAllRead(UUID userId, Instant now) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Notification> unreadOlderThan(UUID userId, Instant before) {
            unreadCalled = true;
            unreadUserId = userId;
            unreadBefore = before;
            return unread;
        }

        @Override
        public void recordDelivery(DeliveryAttempt attempt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean reminderExists(UUID taskId, UUID userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recordReminder(ReminderState state) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ReminderCandidate> dueReminderCandidates(Instant now, int limit) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakeEmailSender implements EmailSender {
        private int sendCount;
        private NotificationPreference sentPreference;
        private List<Notification> sentNotifications = List.of();

        @Override
        public void sendDigest(NotificationPreference preference, List<Notification> notifications) {
            sendCount++;
            sentPreference = preference;
            sentNotifications = notifications;
        }
    }
}
