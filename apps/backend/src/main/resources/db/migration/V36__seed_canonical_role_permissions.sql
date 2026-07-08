INSERT INTO permissions (id, name, description, created_at) VALUES
('20000000-0000-0000-0000-000000000001', 'team.read', 'Read team directory and membership information', CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000002', 'team.manage', 'Manage team membership and settings', CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000003', 'project.read', 'Read project details', CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000004', 'project.create', 'Create projects', CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000005', 'project.update', 'Update project details', CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000006', 'project.archive', 'Archive projects', CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000007', 'project.members.read', 'Read project membership', CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000008', 'project.members.manage', 'Manage project membership', CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000009', 'rbac.roles.read', 'Read role and permission configuration', CURRENT_TIMESTAMP),
('20000000-0000-0000-0000-000000000010', 'rbac.roles.manage', 'Manage role and permission configuration', CURRENT_TIMESTAMP);

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN (
      'team.read', 'team.manage',
      'project.read', 'project.create', 'project.update', 'project.archive',
      'project.members.read', 'project.members.manage',
      'rbac.roles.read', 'rbac.roles.manage'
  );

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.name IN (
      'team.read', 'team.manage',
      'project.read', 'project.create', 'project.update', 'project.archive',
      'project.members.read', 'project.members.manage'
  );

INSERT INTO role_permissions (role_id, permission_id, granted_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MEMBER'
  AND p.name IN ('project.read', 'project.create');
