CREATE TABLE task_types (
    id UUID PRIMARY KEY,
    project_id UUID,
    type_key VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    color VARCHAR(32),
    icon VARCHAR(64),
    system BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_task_types_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT uq_task_types_scope_key UNIQUE (project_id, type_key)
);

CREATE INDEX idx_task_types_project_active ON task_types (project_id, active, sort_order);

INSERT INTO task_types (id, project_id, type_key, name, color, icon, system, active, sort_order, created_at, updated_at, version) VALUES
('00000000-0000-0000-0000-000000000101', NULL, 'EPIC', 'Epic', '#7c3aed', 'flag', TRUE, TRUE, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('00000000-0000-0000-0000-000000000102', NULL, 'STORY', 'Story', '#2563eb', 'book-open', TRUE, TRUE, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('00000000-0000-0000-0000-000000000103', NULL, 'TASK', 'Task', '#64748b', 'check-square', TRUE, TRUE, 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('00000000-0000-0000-0000-000000000104', NULL, 'BUG', 'Bug', '#dc2626', 'bug', TRUE, TRUE, 40, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('00000000-0000-0000-0000-000000000105', NULL, 'SUBTASK', 'Subtask', '#059669', 'list-tree', TRUE, TRUE, 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
('00000000-0000-0000-0000-000000000106', NULL, 'MILESTONE', 'Milestone', '#d97706', 'milestone', TRUE, TRUE, 60, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

ALTER TABLE tasks ADD COLUMN task_type_id UUID;
UPDATE tasks SET task_type_id = (SELECT tt.id FROM task_types tt WHERE tt.project_id IS NULL AND tt.type_key = tasks.task_type);
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_task_type_id FOREIGN KEY (task_type_id) REFERENCES task_types(id);
