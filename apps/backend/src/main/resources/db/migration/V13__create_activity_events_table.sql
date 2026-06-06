CREATE TABLE activity_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    actor_user_id UUID NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID NOT NULL,
    project_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    payload TEXT NOT NULL,
    context TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_activity_events_actor_time ON activity_events(actor_user_id, occurred_at DESC);
CREATE INDEX idx_activity_events_project_time ON activity_events(project_id, occurred_at DESC);
CREATE INDEX idx_activity_events_entity ON activity_events(entity_type, entity_id);
