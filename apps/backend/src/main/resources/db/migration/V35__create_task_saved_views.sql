CREATE TABLE task_saved_views (
    id UUID PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    user_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    filters_json TEXT NOT NULL,
    built_in BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_task_saved_views_user_created ON task_saved_views (user_id, created_at);
