package com.taskmind.backend.aiworkflow.application;

import com.taskmind.backend.aiworkflow.domain.model.AiWorkflowTemplate;
import com.taskmind.backend.aiworkflow.domain.model.ApprovalPolicy;
import com.taskmind.backend.aiworkflow.domain.model.WorkflowType;
import com.taskmind.backend.aiworkflow.domain.repository.AiWorkflowTemplateRepository;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import com.taskmind.backend.project.domain.repository.ProjectMembershipRepository;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AiWorkflowTemplateApplicationService {
    private static final String DEFAULT_ALLOWED_TOOLS = "[]";
    private static final String DEFAULT_MODEL_POLICY = "{}";

    private final AiWorkflowTemplateRepository templates;
    private final ProjectRepository projects;
    private final ProjectMembershipRepository memberships;

    public AiWorkflowTemplateApplicationService(
            AiWorkflowTemplateRepository templates,
            ProjectRepository projects,
            ProjectMembershipRepository memberships) {
        this.templates = templates;
        this.projects = projects;
        this.memberships = memberships;
    }

    @Transactional
    public AiWorkflowTemplate create(AuthenticatedUser actor, UUID projectId, TemplateCommand command) {
        assertProjectAdmin(actor, projectId);
        Instant now = Instant.now();
        return templates.save(new AiWorkflowTemplate(UUID.randomUUID(), null, projectId, command.name().trim(),
                command.description(), command.workflowType(), command.templateBody().trim(),
                normalize(command.allowedTools(), DEFAULT_ALLOWED_TOOLS), command.approvalPolicy(),
                normalize(command.defaultModelPolicy(), DEFAULT_MODEL_POLICY), null, now, now));
    }

    public List<AiWorkflowTemplate> list(AuthenticatedUser actor, UUID projectId) {
        assertProjectMember(actor, projectId);
        return templates.findActiveByProjectId(projectId);
    }

    public Optional<AiWorkflowTemplate> get(AuthenticatedUser actor, UUID templateId) {
        return templates.findById(templateId).filter(template -> canRead(actor, template));
    }

    @Transactional
    public Optional<AiWorkflowTemplate> update(AuthenticatedUser actor, UUID templateId, TemplateCommand command) {
        return templates.findById(templateId).filter(template -> canAdmin(actor, template.projectId()))
                .filter(template -> !template.archived())
                .map(existing -> {
                    if (command.version() != null && !command.version().equals(existing.version())) {
                        throw new org.springframework.dao.OptimisticLockingFailureException("Stale workflow template version");
                    }
                    return templates.save(new AiWorkflowTemplate(existing.id(), existing.version(),
                        existing.projectId(), command.name().trim(), command.description(), command.workflowType(),
                        command.templateBody().trim(), normalize(command.allowedTools(), existing.allowedTools()),
                        command.approvalPolicy(), normalize(command.defaultModelPolicy(), existing.defaultModelPolicy()),
                        existing.archivedAt(), existing.createdAt(), Instant.now()));
                });
    }

    @Transactional
    public boolean archive(AuthenticatedUser actor, UUID templateId) {
        return templates.findById(templateId).filter(template -> canAdmin(actor, template.projectId()))
                .filter(template -> !template.archived())
                .map(template -> {
                    Instant now = Instant.now();
                    templates.save(new AiWorkflowTemplate(template.id(), template.version(), template.projectId(),
                            template.name(), template.description(), template.workflowType(), template.templateBody(),
                            template.allowedTools(), template.approvalPolicy(), template.defaultModelPolicy(), now,
                            template.createdAt(), now));
                    return true;
                })
                .orElse(false);
    }

    private boolean canRead(AuthenticatedUser actor, AiWorkflowTemplate template) {
        return !template.archived() && canMember(actor, template.projectId());
    }

    private void assertProjectMember(AuthenticatedUser actor, UUID projectId) {
        if (!canMember(actor, projectId)) {
            throw new AiWorkflowTemplateForbiddenException("Actor is not allowed to read workflow templates");
        }
    }

    private void assertProjectAdmin(AuthenticatedUser actor, UUID projectId) {
        if (!canAdmin(actor, projectId)) {
            throw new AiWorkflowTemplateForbiddenException("Actor is not allowed to manage workflow templates");
        }
    }

    private boolean canMember(AuthenticatedUser actor, UUID projectId) {
        return actor.isPrivileged()
                || projects.findById(projectId).map(project -> project.ownerUserId().equals(actor.userId())).orElse(false)
                || memberships.existsByProjectIdAndUserId(projectId, actor.userId());
    }

    private boolean canAdmin(AuthenticatedUser actor, UUID projectId) {
        if (actor.isPrivileged()) {
            return true;
        }
        if (projects.findById(projectId).map(project -> project.ownerUserId().equals(actor.userId())).orElse(false)) {
            return true;
        }
        return memberships.findByProjectIdAndUserId(projectId, actor.userId())
                .map(ProjectMembership::role)
                .map(role -> role == ProjectMembershipRole.ADMIN || role == ProjectMembershipRole.OWNER)
                .orElse(false);
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public record TemplateCommand(String name, String description, WorkflowType workflowType, String templateBody,
            String allowedTools, ApprovalPolicy approvalPolicy, String defaultModelPolicy, Long version) {}
}
