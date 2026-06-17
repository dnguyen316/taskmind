package com.taskmind.backend.notification.infrastructure.persistence.jpa;

import com.taskmind.backend.notification.domain.model.NotificationPreference;
import com.taskmind.backend.notification.domain.repository.NotificationPreferenceRepository;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JpaNotificationPreferenceRepository implements NotificationPreferenceRepository {
    private final JdbcTemplate jdbc;

    public JpaNotificationPreferenceRepository(JdbcTemplate j) {
        jdbc = j;
    }

    public Optional<NotificationPreference> findByUserId(UUID u) {
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
                        u)
                .stream()
                .findFirst();
    }

    public NotificationPreference save(NotificationPreference p) {
        if (findByUserId(p.userId()).isPresent())
            jdbc.update(
                    "update notification_preferences set in_app_enabled=?,email_digest_enabled=?,slack_enabled=?,slack_webhook_url=?,slack_channel=?,updated_at=?,version=version+1 where user_id=?",
                    p.inAppEnabled(),
                    p.emailDigestEnabled(),
                    p.slackEnabled(),
                    p.slackWebhookUrl(),
                    p.slackChannel(),
                    Timestamp.from(p.updatedAt()),
                    p.userId());
        else
            jdbc.update(
                    "insert into notification_preferences(user_id,in_app_enabled,email_digest_enabled,slack_enabled,slack_webhook_url,slack_channel,created_at,updated_at,version) values (?,?,?,?,?,?,?,?,0)",
                    p.userId(),
                    p.inAppEnabled(),
                    p.emailDigestEnabled(),
                    p.slackEnabled(),
                    p.slackWebhookUrl(),
                    p.slackChannel(),
                    Timestamp.from(p.createdAt()),
                    Timestamp.from(p.updatedAt()));
        return findByUserId(p.userId()).orElseThrow();
    }
}
