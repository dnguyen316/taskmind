CREATE TABLE projects (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    key VARCHAR(20) UNIQUE NOT NULL,
    description TEXT,
    owner_user_id UUID NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_projects_owner_user_id_created_at ON projects (owner_user_id, created_at DESC);
CREATE INDEX idx_projects_archived_at ON projects (archived_at);
