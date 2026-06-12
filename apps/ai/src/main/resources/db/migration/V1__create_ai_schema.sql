CREATE SCHEMA IF NOT EXISTS ai;

CREATE TABLE ai.ai_runs (
    id UUID PRIMARY KEY,
    user_id UUID,
    workspace_id VARCHAR(128),
    capability_id VARCHAR(128) NOT NULL,
    provider_id VARCHAR(64) NOT NULL,
    model_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    input_json TEXT,
    output_json TEXT,
    error_code VARCHAR(128),
    error_message TEXT,
    prompt_tokens INTEGER,
    completion_tokens INTEGER,
    total_tokens INTEGER,
    latency_ms BIGINT,
    correlation_id VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_ai_runs_user_created_at ON ai.ai_runs (user_id, created_at DESC);
CREATE INDEX idx_ai_runs_correlation_id ON ai.ai_runs (correlation_id);
CREATE INDEX idx_ai_runs_capability_status ON ai.ai_runs (capability_id, status);
