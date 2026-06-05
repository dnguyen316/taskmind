ALTER TABLE tasks ADD COLUMN task_key VARCHAR(40);
CREATE UNIQUE INDEX ux_tasks_task_key ON tasks (task_key);
CREATE TABLE project_task_sequences (
    project_id UUID PRIMARY KEY,
    next_value BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_project_task_sequences_positive CHECK (next_value > 0)
);
