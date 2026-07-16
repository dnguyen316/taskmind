package com.taskmind.backend.notification.infrastructure.persistence.jpa;

import com.taskmind.backend.notification.domain.model.NotificationPreference;
import com.taskmind.backend.notification.domain.repository.NotificationPreferenceRepository;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaNotificationPreferenceRepository implements NotificationPreferenceRepository {
    private final JdbcTemplate jdbc;

    public JpaNotificationPreferenceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    public Optional<NotificationPreference> findByUserId(UUID userId) {
        return jdbc
                .query(
                        "select * from notification_preferences where user_id=?",
                        (rs, i) ->
                                new NotificationPreference(
                                        (UUID) rs.getObject("user_id"),
                                        rs.getLong("version"),
                                        rs.getBoolean("in_app_enabled"),
                                        rs.getBoolean("email_digest_enabled"),
                                        rs.getBoolean("slack_enabled"),
                                        rs.getString("slack_webhook_url"),
                                        rs.getString("slack_channel"),
                                        rs.getTimestamp("created_at").toInstant(),
                                        rs.getTimestamp("updated_at").toInstant()),
                        userId)
                .stream()
                .findFirst();
    }

    public NotificationPreference save(NotificationPreference preference) {
        if (preference.version() != null) {
            int updated =
                    jdbc.update(
                            "update notification_preferences set in_app_enabled=?,email_digest_enabled=?,slack_enabled=?,slack_webhook_url=?,slack_channel=?,updated_at=?,version=version+1 where user_id=? and version=?",
                            preference.inAppEnabled(),
                            preference.emailDigestEnabled(),
                            preference.slackEnabled(),
                            preference.slackWebhookUrl(),
                            preference.slackChannel(),
                            Timestamp.from(preference.updatedAt()),
                            preference.userId(),
                            preference.version());
            if (updated == 1) return findByUserId(preference.userId()).orElseThrow();
            if (findByUserId(preference.userId()).isPresent()) {
                throw new ObjectOptimisticLockingFailureException(
                        NotificationPreference.class, preference.userId());
            }
        }

        try {
            jdbc.update(
                    "insert into notification_preferences(user_id,in_app_enabled,email_digest_enabled,slack_enabled,slack_webhook_url,slack_channel,created_at,updated_at,version) values (?,?,?,?,?,?,?,?,0)",
                    preference.userId(),
                    preference.inAppEnabled(),
                    preference.emailDigestEnabled(),
                    preference.slackEnabled(),
                    preference.slackWebhookUrl(),
                    preference.slackChannel(),
                    Timestamp.from(preference.createdAt()),
                    Timestamp.from(preference.updatedAt()));
        } catch (DuplicateKeyException e) {
            return findByUserId(preference.userId()).orElseThrow();
        }
        return findByUserId(preference.userId()).orElseThrow();
    }
}
