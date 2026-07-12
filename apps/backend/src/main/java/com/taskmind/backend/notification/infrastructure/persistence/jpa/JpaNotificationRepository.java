package com.taskmind.backend.notification.infrastructure.persistence.jpa;

import com.taskmind.backend.notification.domain.model.*;
import com.taskmind.backend.notification.domain.repository.NotificationRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaNotificationRepository implements NotificationRepository {
    private final JdbcTemplate jdbc;

    public JpaNotificationRepository(JdbcTemplate j) {
        jdbc = j;
    }

    public Notification save(Notification n) {
        if (findById(n.id()).isPresent()) {
            int updated =
                    jdbc.update(
                            "update notifications set read_at=?, version=version+1 where id=? and version=?",
                            n.readAt() == null ? null : Timestamp.from(n.readAt()),
                            n.id(),
                            n.version());
            if (updated == 0) {
                throw new ObjectOptimisticLockingFailureException(Notification.class, n.id());
            }
        } else
            jdbc.update(
                    "insert into notifications(id,recipient_user_id,type,title,body,task_id,action_url,read_at,created_at,version) values (?,?,?,?,?,?,?,?,?,0)",
                    n.id(),
                    n.recipientUserId(),
                    n.type().name(),
                    n.title(),
                    n.body(),
                    n.taskId(),
                    n.actionUrl(),
                    n.readAt() == null ? null : Timestamp.from(n.readAt()),
                    Timestamp.from(n.createdAt()));
        return findById(n.id()).orElseThrow();
    }

    public Optional<Notification> findById(UUID id) {
        List<Notification> l = jdbc.query("select * from notifications where id=?", this::map, id);
        return l.stream().findFirst();
    }

    public List<Notification> findByRecipient(UUID u, int p, int s) {
        return jdbc.query(
                "select * from notifications where recipient_user_id=? order by created_at desc limit ? offset ?",
                this::map,
                u,
                s,
                p * s);
    }

    public long unreadCount(UUID u) {
        Long c =
                jdbc.queryForObject(
                        "select count(*) from notifications where recipient_user_id=? and read_at is null",
                        Long.class,
                        u);
        return c == null ? 0 : c;
    }

    /**
     * Bulk idempotent read operation. It intentionally bypasses per-row optimistic locking so
     * repeated read-all requests can safely converge every unread notification for the user.
     */
    public int markAllRead(UUID u, Instant now) {
        return jdbc.update(
                "update notifications set read_at=?, version=version+1 where recipient_user_id=? and read_at is null",
                Timestamp.from(now),
                u);
    }

    public List<Notification> unreadOlderThan(UUID u, Instant b) {
        return jdbc.query(
                "select * from notifications where recipient_user_id=? and read_at is null and created_at <= ? order by created_at",
                this::map,
                u,
                Timestamp.from(b));
    }

    public void recordDelivery(DeliveryAttempt a) {
        jdbc.update(
                "insert into notification_delivery_attempts(id,notification_id,user_id,channel,status,error_message,attempted_at) values (?,?,?,?,?,?,?)",
                a.id(),
                a.notificationId(),
                a.userId(),
                a.channel().name(),
                a.status().name(),
                a.errorMessage(),
                Timestamp.from(a.attemptedAt()));
    }

    public Optional<DeliveryAttempt> latestDeliveryAttempt(UUID notificationId, NotificationChannel channel) {
        return jdbc.query(
                        "select * from notification_delivery_attempts where notification_id=? and channel=? order by attempted_at desc limit 1",
                        this::mapAttempt,
                        notificationId,
                        channel.name())
                .stream()
                .findFirst();
    }


    public List<DeliveryAttempt> claimPendingDeliveries(Instant now, int limit) {
        return jdbc.query(
                "select * from notification_delivery_attempts where status='PENDING' and attempted_at <= ? order by attempted_at limit ? for update skip locked",
                this::mapAttempt,
                Timestamp.from(now),
                limit);
    }

    public void markDeliverySent(UUID deliveryAttemptId, Instant now) {
        jdbc.update(
                "update notification_delivery_attempts set status='SENT', error_message=null, attempted_at=? where id=?",
                Timestamp.from(now),
                deliveryAttemptId);
    }

    public void markDeliveryFailed(UUID deliveryAttemptId, String errorMessage, Instant retryAt) {
        jdbc.update(
                "update notification_delivery_attempts set status='FAILED', error_message=?, attempted_at=? where id=?",
                errorMessage,
                Timestamp.from(retryAt),
                deliveryAttemptId);
    }

    public boolean reminderExists(UUID t, UUID u) {
        Boolean b =
                jdbc.queryForObject(
                        "select count(*)>0 from notification_reminder_state where task_id=? and user_id=?",
                        Boolean.class,
                        t,
                        u);
        return Boolean.TRUE.equals(b);
    }

    public void recordReminder(ReminderState s) {
        jdbc.update(
                "merge into notification_reminder_state(task_id,user_id,reminded_at) key(task_id,user_id) values (?,?,?)",
                s.taskId(),
                s.userId(),
                Timestamp.from(s.remindedAt()));
    }

    public List<ReminderCandidate> dueReminderCandidates(Instant now, int limit) {
        return jdbc.query(
                "select id,user_id,assignee_id,title,due_at from tasks where due_at is not null and due_at <= ? and status <> 'DONE' and deleted_at is null limit ?",
                (rs, i) ->
                        new ReminderCandidate(
                                (UUID) rs.getObject("id"),
                                (UUID) rs.getObject("user_id"),
                                (UUID) rs.getObject("assignee_id"),
                                rs.getString("title"),
                                rs.getTimestamp("due_at").toInstant()),
                Timestamp.from(now),
                limit);
    }

    private DeliveryAttempt mapAttempt(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        return new DeliveryAttempt(
                (UUID) rs.getObject("id"),
                (UUID) rs.getObject("notification_id"),
                (UUID) rs.getObject("user_id"),
                NotificationChannel.valueOf(rs.getString("channel")),
                DeliveryStatus.valueOf(rs.getString("status")),
                rs.getString("error_message"),
                rs.getTimestamp("attempted_at").toInstant());
    }

    private Notification map(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        Timestamp r = rs.getTimestamp("read_at");
        return new Notification(
                (UUID) rs.getObject("id"),
                rs.getLong("version"),
                (UUID) rs.getObject("recipient_user_id"),
                NotificationType.valueOf(rs.getString("type")),
                rs.getString("title"),
                rs.getString("body"),
                (UUID) rs.getObject("task_id"),
                rs.getString("action_url"),
                r != null,
                r == null ? null : r.toInstant(),
                rs.getTimestamp("created_at").toInstant());
    }
}
