CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    stream_key VARCHAR(160) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    publish_attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_outbox_events_unpublished ON outbox_events(published_at, occurred_at);
