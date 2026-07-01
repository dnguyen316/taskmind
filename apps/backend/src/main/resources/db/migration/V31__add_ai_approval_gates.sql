ALTER TABLE ai_workflow_templates ADD COLUMN auto_approve_read_only BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_workflow_templates ADD COLUMN require_approval_for_comments BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ai_workflow_templates ADD COLUMN require_approval_for_branch BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_workflow_templates ADD COLUMN require_approval_for_pull_request BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_workflow_templates ADD COLUMN require_approval_for_task_mutation BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE ai_workflow_template_versions ADD COLUMN auto_approve_read_only BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_workflow_template_versions ADD COLUMN require_approval_for_comments BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ai_workflow_template_versions ADD COLUMN require_approval_for_branch BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_workflow_template_versions ADD COLUMN require_approval_for_pull_request BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE ai_workflow_template_versions ADD COLUMN require_approval_for_task_mutation BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE ai_task_resolution_action_proposals (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES ai_task_resolution_jobs(id) ON DELETE CASCADE,
    proposed_action_type VARCHAR(80) NOT NULL,
    payload_preview TEXT NOT NULL DEFAULT '{}',
    risk_level VARCHAR(40) NOT NULL,
    rationale TEXT,
    status VARCHAR(40) NOT NULL,
    decided_by UUID REFERENCES users(id),
    decided_at TIMESTAMP WITH TIME ZONE,
    error_code VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ai_task_resolution_action_proposal_status CHECK (status IN ('PENDING','APPROVED','REJECTED','EXECUTED','FAILED'))
);

CREATE INDEX idx_ai_task_resolution_action_proposals_job ON ai_task_resolution_action_proposals(job_id, created_at);
