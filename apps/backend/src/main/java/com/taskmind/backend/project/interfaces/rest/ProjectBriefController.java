package com.taskmind.backend.project.interfaces.rest;

import com.taskmind.backend.ai.application.AiFacadeApplicationService;
import com.taskmind.backend.ai.application.ProjectBriefResult;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.application.ProjectApplicationService;
import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.project.domain.model.Project;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/projects/{projectId}/ai-brief")
public class ProjectBriefController {
    private final ProjectApplicationService projects;
    private final ProjectMembershipApplicationService memberships;
    private final AiFacadeApplicationService aiFacade;

    public ProjectBriefController(
            ProjectApplicationService projects,
            ProjectMembershipApplicationService memberships,
            AiFacadeApplicationService aiFacade) {
        this.projects = projects;
        this.memberships = memberships;
        this.aiFacade = aiFacade;
    }

    @PostMapping
    public ResponseEntity<ProjectBriefResponse> brief(
            AuthenticatedUser requester, @PathVariable UUID projectId) {
        return projects.findById(projectId)
                .filter(project -> canRead(requester, project))
                .map(project -> aiFacade.projectBrief(
                                requester.userId(),
                                project.id(),
                                project.name(),
                                project.description()))
                .map(ProjectBriefResponse::fromResult)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean canRead(AuthenticatedUser requester, Project project) {
        return requester.isPrivileged()
                || project.ownerUserId().equals(requester.userId())
                || memberships.isMember(project.id(), requester.userId());
    }

    public record ProjectBriefResponse(
            UUID projectId,
            String summary,
            List<String> currentFocus,
            List<String> risks,
            List<String> suggestedNextSteps) {
        static ProjectBriefResponse fromResult(ProjectBriefResult result) {
            return new ProjectBriefResponse(
                    result.projectId(),
                    result.summary(),
                    result.currentFocus(),
                    result.risks(),
                    result.suggestedNextSteps());
        }
    }
}
