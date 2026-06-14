CREATE TABLE IF NOT EXISTS analytics.ai_funnel_daily_metrics (
    user_id UUID NOT NULL,
    metric_date DATE NOT NULL,
    captures_submitted INTEGER NOT NULL DEFAULT 0,
    suggestions_accepted INTEGER NOT NULL DEFAULT 0,
    suggestions_rejected INTEGER NOT NULL DEFAULT 0,
    events_ingested INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, metric_date)
);
