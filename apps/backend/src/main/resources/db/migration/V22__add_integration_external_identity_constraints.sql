ALTER TABLE integration_external_links
  ADD CONSTRAINT uk_integration_external_links_external_id UNIQUE(provider, external_type, external_id);

ALTER TABLE integration_external_links
  ADD CONSTRAINT uk_integration_external_links_external_key UNIQUE(provider, external_type, external_key);
