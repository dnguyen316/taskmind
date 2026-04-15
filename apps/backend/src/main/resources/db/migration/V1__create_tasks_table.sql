CREATE TABLE tasks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    project_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL,
    priority INTEGER NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    duration_minutes INTEGER,
    energy_level VARCHAR(30),
    source VARCHAR(30) NOT NULL,
    confidence NUMERIC(4, 3),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_tasks_priority_range CHECK (priority BETWEEN 1 AND 4),
    CONSTRAINT chk_tasks_confidence_range CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

CREATE INDEX idx_tasks_user_id_created_at ON tasks (user_id, created_at DESC);
CREATE INDEX idx_tasks_status ON tasks (status);
CREATE INDEX idx_tasks_due_at ON tasks (due_at);
