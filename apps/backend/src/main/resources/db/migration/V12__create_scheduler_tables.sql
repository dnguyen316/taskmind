CREATE TABLE scheduling_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE,
    workday_start TIME NOT NULL,
    workday_end TIME NOT NULL,
    block_granularity_minutes INTEGER NOT NULL,
    max_daily_focus_minutes INTEGER NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_scheduling_preferences_window CHECK (workday_start < workday_end),
    CONSTRAINT chk_scheduling_preferences_granularity CHECK (block_granularity_minutes BETWEEN 15 AND 240),
    CONSTRAINT chk_scheduling_preferences_focus CHECK (max_daily_focus_minutes BETWEEN 15 AND 1440)
);

CREATE INDEX idx_scheduling_preferences_user_id ON scheduling_preferences (user_id);

CREATE TABLE scheduled_blocks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    task_id UUID NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL,
    rationale VARCHAR(500),
    completed_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_scheduled_blocks_task FOREIGN KEY (task_id) REFERENCES tasks (id),
    CONSTRAINT chk_scheduled_blocks_window CHECK (starts_at < ends_at),
    CONSTRAINT chk_scheduled_blocks_status CHECK (status IN ('SCHEDULED', 'COMPLETED', 'MISSED', 'CANCELLED'))
);

CREATE INDEX idx_scheduled_blocks_user_start ON scheduled_blocks (user_id, starts_at);
CREATE INDEX idx_scheduled_blocks_task_id ON scheduled_blocks (task_id);
CREATE INDEX idx_scheduled_blocks_status ON scheduled_blocks (status);
