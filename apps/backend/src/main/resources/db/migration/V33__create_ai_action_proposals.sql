CREATE TABLE ai_action_proposals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action_type VARCHAR(60) NOT NULL,
    status VARCHAR(40) NOT NULL,
    proposed_payload TEXT NOT NULL DEFAULT '{}',
    preview TEXT NOT NULL DEFAULT '',
    rationale TEXT,
    proposer VARCHAR(120) NOT NULL,
    provider VARCHAR(120),
    model VARCHAR(120),
    source VARCHAR(80) NOT NULL,
    source_context TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    accepted_at TIMESTAMP WITH TIME ZONE,
    decided_by UUID REFERENCES users(id),
    user_decision TEXT,
    CONSTRAINT ck_ai_action_proposals_type CHECK (action_type IN ('CREATE_TASK','UPDATE_TASK','SCHEDULE_TASK','CREATE_PROJECT','ADD_SUBTASKS','ARCHIVE_TASK','ASSIGN_TASK')),
    CONSTRAINT ck_ai_action_proposals_status CHECK (status IN ('PENDING','ACCEPTED','EDITED','REJECTED','EXPIRED')),
    CONSTRAINT ck_ai_action_proposals_source CHECK (source IN ('AI_CAPTURE','GOAL_BREAKDOWN','SPEC_BREAKDOWN','SCHEDULER','NOVA_CHAT','TASK_RESOLUTION'))
);

CREATE INDEX idx_ai_action_proposals_pending ON ai_action_proposals(user_id, status, created_at);
