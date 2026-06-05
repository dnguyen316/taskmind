ALTER TABLE tasks ADD COLUMN parent_task_id UUID;
ALTER TABLE tasks ADD COLUMN task_level VARCHAR(20) NOT NULL DEFAULT 'TASK';
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE SET NULL;
ALTER TABLE tasks ADD CONSTRAINT chk_tasks_task_level CHECK (task_level IN ('EPIC', 'STORY', 'TASK', 'SUBTASK'));
ALTER TABLE tasks ADD CONSTRAINT chk_tasks_not_own_parent CHECK (parent_task_id IS NULL OR parent_task_id <> id);
CREATE INDEX idx_tasks_parent_task_id ON tasks (parent_task_id);
