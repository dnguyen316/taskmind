ALTER TABLE tasks ADD COLUMN task_type VARCHAR(20) NOT NULL DEFAULT 'TASK';
ALTER TABLE tasks ADD CONSTRAINT chk_tasks_task_type CHECK (task_type IN ('EPIC', 'STORY', 'TASK', 'BUG', 'SUBTASK', 'MILESTONE'));
ALTER TABLE tasks ADD CONSTRAINT chk_tasks_project_required_for_hierarchy CHECK (parent_task_id IS NULL OR project_id IS NOT NULL);
CREATE TABLE task_links (
    id UUID PRIMARY KEY,
    source_task_id UUID NOT NULL,
    target_task_id UUID NOT NULL,
    link_type VARCHAR(20) NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_task_links_source FOREIGN KEY (source_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_links_target FOREIGN KEY (target_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT chk_task_links_distinct_tasks CHECK (source_task_id <> target_task_id),
    CONSTRAINT chk_task_links_type CHECK (link_type IN ('BLOCKS', 'RELATES_TO', 'DUPLICATES')),
    CONSTRAINT uq_task_links UNIQUE (source_task_id, target_task_id, link_type)
);
CREATE INDEX idx_task_links_target ON task_links (target_task_id);
