ALTER TABLE integration_project_links ADD COLUMN repository_owner VARCHAR(255);
ALTER TABLE integration_project_links ADD COLUMN repository_name VARCHAR(255);
ALTER TABLE integration_project_links ADD COLUMN default_branch VARCHAR(255);
ALTER TABLE integration_project_links ADD COLUMN installation_id VARCHAR(255);
ALTER TABLE integration_project_links ADD COLUMN account_id VARCHAR(255);
ALTER TABLE integration_project_links ADD COLUMN allowed_operations_json TEXT;

CREATE INDEX idx_integration_project_links_github_repo
  ON integration_project_links(provider, repository_owner, repository_name);

ALTER TABLE integration_external_links ADD COLUMN repository_owner VARCHAR(255);
ALTER TABLE integration_external_links ADD COLUMN repository_name VARCHAR(255);
ALTER TABLE integration_external_links ADD COLUMN external_number INTEGER;
ALTER TABLE integration_external_links ADD COLUMN git_sha VARCHAR(128);
ALTER TABLE integration_external_links ADD COLUMN check_run_id VARCHAR(255);

CREATE INDEX idx_integration_external_links_github_repo
  ON integration_external_links(provider, repository_owner, repository_name, external_type);
CREATE INDEX idx_integration_external_links_git_sha
  ON integration_external_links(provider, git_sha);
