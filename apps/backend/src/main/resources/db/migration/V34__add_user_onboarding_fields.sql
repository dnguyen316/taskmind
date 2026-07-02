ALTER TABLE users ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN onboarding_workspace_type VARCHAR(40);
ALTER TABLE users ADD COLUMN onboarding_planning_style VARCHAR(40);
