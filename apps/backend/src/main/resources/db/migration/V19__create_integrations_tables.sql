CREATE TABLE integration_connections (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  provider VARCHAR(32) NOT NULL,
  account_name VARCHAR(255) NOT NULL,
  base_url VARCHAR(500),
  account_external_id VARCHAR(255),
  owner_user_id UUID NOT NULL,
  encrypted_access_token TEXT NOT NULL,
  encrypted_refresh_token TEXT,
  token_expires_at TIMESTAMP WITH TIME ZONE,
  scopes TEXT,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_integration_connections_owner ON integration_connections(owner_user_id, provider);

CREATE TABLE integration_oauth_states (
  state VARCHAR(128) PRIMARY KEY,
  provider VARCHAR(32) NOT NULL,
  owner_user_id UUID NOT NULL,
  redirect_uri VARCHAR(1000),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE integration_project_links (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  project_id UUID NOT NULL REFERENCES projects(id),
  connection_id UUID NOT NULL REFERENCES integration_connections(id),
  provider VARCHAR(32) NOT NULL,
  external_project_id VARCHAR(255) NOT NULL,
  external_project_key VARCHAR(255),
  external_project_name VARCHAR(255),
  metadata_json TEXT,
  created_by UUID NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE(project_id, provider, external_project_id)
);
CREATE INDEX idx_integration_project_links_project ON integration_project_links(project_id);

CREATE TABLE integration_import_runs (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  project_id UUID NOT NULL REFERENCES projects(id),
  project_link_id UUID NOT NULL REFERENCES integration_project_links(id),
  provider VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  imported_count INTEGER NOT NULL,
  skipped_count INTEGER NOT NULL,
  error_message TEXT,
  requested_by UUID NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE integration_external_links (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  task_id UUID NOT NULL REFERENCES tasks(id),
  project_id UUID NOT NULL REFERENCES projects(id),
  provider VARCHAR(32) NOT NULL,
  external_type VARCHAR(64) NOT NULL,
  external_id VARCHAR(255) NOT NULL,
  external_key VARCHAR(255),
  external_url VARCHAR(1000),
  direction VARCHAR(32) NOT NULL,
  metadata_json TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE(provider, external_id, task_id)
);
CREATE INDEX idx_integration_external_links_task ON integration_external_links(task_id);
CREATE INDEX idx_integration_external_links_project ON integration_external_links(project_id);

CREATE TABLE integration_publish_records (
  id UUID PRIMARY KEY,
  version BIGINT NOT NULL DEFAULT 0,
  task_id UUID NOT NULL REFERENCES tasks(id),
  project_link_id UUID NOT NULL REFERENCES integration_project_links(id),
  provider VARCHAR(32) NOT NULL,
  external_id VARCHAR(255) NOT NULL,
  external_key VARCHAR(255) NOT NULL,
  external_url VARCHAR(1000),
  status VARCHAR(32) NOT NULL,
  published_by UUID NOT NULL,
  published_at TIMESTAMP WITH TIME ZONE NOT NULL,
  metadata_json TEXT,
  UNIQUE(task_id, provider, external_id)
);
