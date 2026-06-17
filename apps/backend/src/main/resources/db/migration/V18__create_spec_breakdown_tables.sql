CREATE TABLE spec_breakdown_drafts (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    owner_user_id UUID NOT NULL,
    template_id UUID,
    title VARCHAR(255) NOT NULL,
    raw_spec TEXT NOT NULL,
    rich_content TEXT,
    candidate_tree TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    fix_version VARCHAR(128),
    affected_version VARCHAR(128),
    sprint VARCHAR(128),
    issue_type VARCHAR(64),
    publish_key VARCHAR(128),
    materialized_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE spec_breakdown_jobs (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL REFERENCES spec_breakdown_drafts(id),
    user_id UUID NOT NULL,
    ai_job_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    checkpoint TEXT,
    nova_run_id UUID,
    error_message TEXT,
    requested_cancel BOOLEAN NOT NULL DEFAULT FALSE,
    paused BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE spec_breakdown_templates (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    fields TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE spec_breakdown_attachments (
    id UUID PRIMARY KEY,
    draft_id UUID NOT NULL REFERENCES spec_breakdown_drafts(id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(160) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

ALTER TABLE tasks ADD COLUMN spec_breakdown_draft_id UUID;
ALTER TABLE tasks ADD COLUMN jira_fix_version VARCHAR(128);
ALTER TABLE tasks ADD COLUMN jira_affected_version VARCHAR(128);
ALTER TABLE tasks ADD COLUMN jira_sprint VARCHAR(128);
ALTER TABLE tasks ADD COLUMN jira_issue_type VARCHAR(64);
ALTER TABLE tasks ADD COLUMN jira_publish_key VARCHAR(128);
