package com.taskmind.backend.team.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.auth.application.GlobalRoleAssignmentService;
import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.project.domain.model.ProjectMembership;
import org.springframework.stereotype.Service;

@Service
public class TeamMembershipApplicationService {
    private final ProjectMembershipApplicationService projectMemberships;
    private final GlobalRoleAssignmentService globalRoles;

    public TeamMembershipApplicationService(
            ProjectMembershipApplicationService projectMemberships,
            GlobalRoleAssignmentService globalRoles) {
        this.projectMemberships = projectMemberships;
        this.globalRoles = globalRoles;
    }

    public ProjectMembership assignProjectMember(AuthenticatedUser actor, AssignProjectMemberCommand command) {
        assertCanManageProjectMembers(actor);
        return projectMemberships.addMember(actor, command.projectId(), command.userId(), command.role());
    }

    public ProjectMembership changeProjectMemberRole(AuthenticatedUser actor, ChangeProjectMemberRoleCommand command) {
        assertCanManageProjectMembers(actor);
        return projectMemberships.changeMemberRole(actor, command.projectId(), command.userId(), command.role());
    }

    public void removeProjectMember(AuthenticatedUser actor, RemoveProjectMemberCommand command) {
        assertCanManageProjectMembers(actor);
        projectMemberships.removeMember(actor, command.projectId(), command.userId());
    }

    public String changeGlobalRole(AuthenticatedUser actor, ChangeGlobalRoleCommand command) {
        return globalRoles.changeRole(actor, command.userId(), command.role());
    }

    private void assertCanManageProjectMembers(AuthenticatedUser actor) {
        if (!actor.hasAnyPermission("team.manage", "project.members.manage")) {
            throw new SecurityException("Project membership management requires team.manage or project.members.manage");
        }
    }
}
