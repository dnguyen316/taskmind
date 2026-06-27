CREATE TABLE ai_workflow_templates (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    workflow_type VARCHAR(64) NOT NULL,
    template_body TEXT NOT NULL,
    allowed_tools TEXT NOT NULL DEFAULT '[]',
    approval_policy VARCHAR(64) NOT NULL,
    default_model_policy TEXT NOT NULL DEFAULT '{}',
    archived_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ai_workflow_templates_project_active
    ON ai_workflow_templates(project_id, archived_at, updated_at DESC);

CREATE TABLE ai_workflow_template_versions (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES ai_workflow_templates(id) ON DELETE CASCADE,
    template_version BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    workflow_type VARCHAR(64) NOT NULL,
    template_body TEXT NOT NULL,
    allowed_tools TEXT NOT NULL,
    approval_policy VARCHAR(64) NOT NULL,
    default_model_policy TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(template_id, template_version)
);

CREATE TABLE ai_workflow_template_bindings (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES ai_workflow_templates(id) ON DELETE CASCADE,
    project_id UUID NOT NULL REFERENCES projects(id),
    binding_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE(project_id, binding_key)
);
