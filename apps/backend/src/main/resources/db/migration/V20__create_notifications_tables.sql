CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    recipient_user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT,
    task_id UUID,
    action_url VARCHAR(512),
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_user_id, created_at DESC);
CREATE INDEX idx_notifications_recipient_unread ON notifications (recipient_user_id, read_at);

CREATE TABLE notification_preferences (
    user_id UUID PRIMARY KEY,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_digest_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    slack_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    slack_webhook_url VARCHAR(1024),
    slack_channel VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE notification_delivery_attempts (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    channel VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    error_message TEXT,
    attempted_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_notification_delivery_attempts_notification ON notification_delivery_attempts(notification_id);

CREATE TABLE notification_subscriptions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    client_id VARCHAR(128),
    connected_at TIMESTAMP WITH TIME ZONE NOT NULL,
    disconnected_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_notification_subscriptions_user ON notification_subscriptions(user_id, connected_at DESC);

CREATE TABLE notification_reminder_state (
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    reminded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (task_id, user_id)
);
