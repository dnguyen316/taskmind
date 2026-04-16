CREATE TABLE project_memberships (
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    CONSTRAINT pk_project_memberships PRIMARY KEY (project_id, user_id),
    CONSTRAINT chk_project_memberships_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER', 'VIEWER'))
);
