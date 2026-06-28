CREATE TABLE ai_task_resolution_jobs (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL REFERENCES tasks(id),
    project_id UUID NOT NULL REFERENCES projects(id),
    template_id UUID REFERENCES ai_workflow_templates(id),
    github_project_link_id UUID REFERENCES integration_project_links(id),
    status VARCHAR(40) NOT NULL,
    requested_by UUID NOT NULL REFERENCES users(id),
    idempotency_key VARCHAR(160),
    nova_run_id UUID,
    current_step VARCHAR(120),
    result_summary TEXT,
    error_code VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_ai_task_resolution_jobs_status CHECK (status IN ('QUEUED','RUNNING','WAITING_FOR_APPROVAL','SUCCEEDED','FAILED','CANCELED','PAUSED')),
    CONSTRAINT uq_ai_task_resolution_idempotency UNIQUE (task_id, requested_by, idempotency_key)
);

CREATE INDEX idx_ai_task_resolution_jobs_task ON ai_task_resolution_jobs(task_id, created_at DESC);
CREATE INDEX idx_ai_task_resolution_jobs_status ON ai_task_resolution_jobs(status, created_at);

CREATE TABLE ai_task_resolution_checkpoints (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES ai_task_resolution_jobs(id) ON DELETE CASCADE,
    step VARCHAR(120) NOT NULL,
    payload TEXT NOT NULL DEFAULT '{}',
    idempotency_key VARCHAR(160),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (job_id, step, idempotency_key)
);

CREATE TABLE ai_task_resolution_approvals (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES ai_task_resolution_jobs(id) ON DELETE CASCADE,
    requested_step VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    decided_by UUID REFERENCES users(id),
    decided_at TIMESTAMP WITH TIME ZONE,
    comment TEXT,
    CONSTRAINT ck_ai_task_resolution_approvals_status CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELED'))
);
