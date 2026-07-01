CREATE UNIQUE INDEX ux_task_types_global_type_key ON task_types (type_key) WHERE project_id IS NULL;
