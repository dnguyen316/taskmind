CREATE SCHEMA IF NOT EXISTS analytics;

CREATE TABLE analytics.event_store (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(120) NOT NULL,
    actor_user_id UUID NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    project_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload TEXT NOT NULL,
    context TEXT NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE analytics.user_daily_metrics (
    user_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    tasks_created INTEGER NOT NULL DEFAULT 0,
    tasks_completed INTEGER NOT NULL DEFAULT 0,
    projects_created INTEGER NOT NULL DEFAULT 0,
    events_ingested INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, metric_date)
);

CREATE TABLE analytics.project_daily_metrics (
    project_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    tasks_created INTEGER NOT NULL DEFAULT 0,
    tasks_completed INTEGER NOT NULL DEFAULT 0,
    projects_updated INTEGER NOT NULL DEFAULT 0,
    events_ingested INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (project_id, metric_date)
);

CREATE TABLE analytics.task_projection (
    task_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    project_id UUID,
    title VARCHAR(500) NOT NULL,
    status VARCHAR(80) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE analytics.project_projection (
    project_id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    project_key VARCHAR(32) NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE analytics.relay_dlq (
    id UUID PRIMARY KEY,
    event_id UUID,
    event_type VARCHAR(120),
    payload TEXT NOT NULL,
    error_message TEXT NOT NULL,
    failed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
