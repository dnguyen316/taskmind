ALTER TABLE task_types ADD COLUMN default_task_level VARCHAR(32);
ALTER TABLE task_types ADD COLUMN allowed_task_levels VARCHAR(128);
ALTER TABLE task_types ADD COLUMN is_container BOOLEAN;
ALTER TABLE task_types ADD COLUMN allow_children BOOLEAN;
ALTER TABLE task_types ADD COLUMN system_kind VARCHAR(32);

UPDATE task_types
SET default_task_level = CASE type_key
        WHEN 'EPIC' THEN 'EPIC'
        WHEN 'STORY' THEN 'STORY'
        WHEN 'SUBTASK' THEN 'SUBTASK'
        ELSE 'TASK'
    END,
    allowed_task_levels = CASE type_key
        WHEN 'EPIC' THEN 'EPIC'
        WHEN 'STORY' THEN 'STORY'
        WHEN 'SUBTASK' THEN 'SUBTASK'
        WHEN 'MILESTONE' THEN 'EPIC,STORY,TASK'
        ELSE 'TASK'
    END,
    is_container = type_key IN ('EPIC', 'STORY', 'MILESTONE'),
    allow_children = type_key IN ('EPIC', 'STORY', 'MILESTONE'),
    system_kind = CASE WHEN system THEN type_key ELSE NULL END;

ALTER TABLE task_types ALTER COLUMN default_task_level SET NOT NULL;
ALTER TABLE task_types ALTER COLUMN allowed_task_levels SET NOT NULL;
ALTER TABLE task_types ALTER COLUMN is_container SET NOT NULL;
ALTER TABLE task_types ALTER COLUMN allow_children SET NOT NULL;

ALTER TABLE tasks DROP CONSTRAINT chk_tasks_task_type;
