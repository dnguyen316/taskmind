ALTER TABLE outbox_events ADD COLUMN claimed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE outbox_events ADD COLUMN claimed_by VARCHAR(120);

CREATE INDEX idx_outbox_events_claimable ON outbox_events(published_at, claimed_at, occurred_at);
CREATE INDEX idx_outbox_events_claimed_by ON outbox_events(claimed_by, published_at, occurred_at);
