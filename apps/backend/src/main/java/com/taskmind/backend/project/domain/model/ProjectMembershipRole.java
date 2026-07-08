package com.taskmind.backend.project.domain.model;

public enum ProjectMembershipRole {
    OWNER(true, true, true, true, true),
    ADMIN(true, true, true, true, true),
    MEMBER(true, false, false, true, false),
    VIEWER(true, false, false, true, false);

    private final boolean canReadProject;
    private final boolean canUpdateProject;
    private final boolean canArchiveProject;
    private final boolean canListMembers;
    private final boolean canManageMembers;

    ProjectMembershipRole(
            boolean canReadProject,
            boolean canUpdateProject,
            boolean canArchiveProject,
            boolean canListMembers,
            boolean canManageMembers) {
        this.canReadProject = canReadProject;
        this.canUpdateProject = canUpdateProject;
        this.canArchiveProject = canArchiveProject;
        this.canListMembers = canListMembers;
        this.canManageMembers = canManageMembers;
    }

    public boolean canReadProject() {
        return canReadProject;
    }

    public boolean canUpdateProject() {
        return canUpdateProject;
    }

    public boolean canArchiveProject() {
        return canArchiveProject;
    }

    public boolean canListMembers() {
        return canListMembers;
    }

    public boolean canManageMembers() {
        return canManageMembers;
    }

    public boolean canMutateProjectSettings() {
        return canUpdateProject && canArchiveProject;
    }
}
