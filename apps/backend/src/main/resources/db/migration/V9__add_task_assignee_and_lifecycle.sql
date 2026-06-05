ALTER TABLE tasks ADD COLUMN assignee_id UUID;
ALTER TABLE tasks ADD COLUMN story_points INTEGER;
ALTER TABLE tasks ADD COLUMN release_version VARCHAR(100);
ALTER TABLE tasks ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tasks ADD CONSTRAINT chk_tasks_story_points_non_negative CHECK (story_points IS NULL OR story_points >= 0);
CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);
CREATE INDEX idx_tasks_project_release ON tasks (project_id, release_version, status);
