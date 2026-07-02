package com.taskmind.backend.onboarding.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaRepository;
import com.taskmind.backend.project.application.CreateProjectCommand;
import com.taskmind.backend.project.application.ProjectApplicationService;
import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.task.application.CreateTaskCommand;
import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.domain.model.EnergyLevel;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingApplicationService {
    private final UserJpaRepository users;
    private final ProjectApplicationService projects;
    private final TaskApplicationService tasks;
    private final JdbcTemplate jdbc;
    private final Environment environment;

    public OnboardingApplicationService(UserJpaRepository users, ProjectApplicationService projects, TaskApplicationService tasks, JdbcTemplate jdbc, Environment environment) {
        this.users = users;
        this.projects = projects;
        this.tasks = tasks;
        this.jdbc = jdbc;
        this.environment = environment;
    }

    @Transactional
    public OnboardingResult complete(AuthenticatedUser user, String workspaceType, String planningStyle, String startMode, String templateKey) {
        com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaEntity entity = users.findById(user.userId()).orElseThrow();
        Project project = switch (normalize(startMode)) {
            case "DEMO" -> createDemoWorkspace(user);
            case "TEMPLATE" -> createTemplateWorkspace(user, templateKey);
            default -> createBlankWorkspace(user, workspaceType);
        };
        entity.completeOnboarding(normalize(workspaceType), normalize(planningStyle), Instant.now());
        return new OnboardingResult(true, project.id());
    }

    @Transactional
    public OnboardingResult resetDemoWorkspace(AuthenticatedUser user) {
        if (!demoResetAllowed()) throw new IllegalStateException("Demo reset is only available in local or demo profiles");
        jdbc.update("delete from activity_events where actor_user_id=?", user.userId());
        jdbc.update("delete from scheduled_blocks where user_id=?", user.userId());
        jdbc.update("delete from tasks where user_id=?", user.userId());
        jdbc.update("delete from projects where owner_user_id=? and project_key like 'DEMO%'", user.userId());
        users.findById(user.userId()).ifPresent(u -> u.resetOnboarding(Instant.now()));
        return complete(user, "TEAM", "SPRINT", "DEMO", null);
    }

    public List<TemplateSummary> templates() {
        return List.of(
                new TemplateSummary("product-launch", "Product launch", "Launch plan with GTM, beta feedback, and release readiness tasks."),
                new TemplateSummary("engineering-sprint", "Engineering sprint", "Sprint kickoff, implementation, QA, and retro workflow."),
                new TemplateSummary("personal-productivity", "Personal productivity", "Focus-friendly personal goals, habits, and weekly review."),
                new TemplateSummary("bug-triage", "Bug triage", "Intake, reproduce, prioritize, fix, and verification flow."),
                new TemplateSummary("hiring-plan", "Hiring plan", "Role definition, sourcing, interviews, and offer process."));
    }

    private Project createBlankWorkspace(AuthenticatedUser user, String workspaceType) {
        return projects.create(new CreateProjectCommand("My TaskMind Workspace", uniqueKey("HOME"), "Created during onboarding for " + workspaceType, user.userId()));
    }

    private Project createTemplateWorkspace(AuthenticatedUser user, String templateKey) {
        TemplateSummary template = templates().stream().filter(t -> t.key().equalsIgnoreCase(String.valueOf(templateKey))).findFirst().orElse(templates().get(0));
        Project project = projects.create(new CreateProjectCommand(template.name(), uniqueKey(template.key().replace("-", "").substring(0, Math.min(4, template.key().length()))), template.description(), user.userId()));
        createTask(user, project.id(), "Review " + template.name() + " goals", "Confirm scope and success criteria.", 1);
        createTask(user, project.id(), "Plan next milestones", "Break the template into this week's work.", 2);
        createTask(user, project.id(), "Share progress update", "Prepare a short stakeholder summary.", 3);
        return project;
    }

    private Project createDemoWorkspace(AuthenticatedUser user) {
        Project project = projects.create(new CreateProjectCommand("TaskMind Demo Project", uniqueKey("DEMO"), "Sample workspace with tasks, schedule blocks, activity, and report data.", user.userId()));
        com.taskmind.backend.task.domain.model.Task t1 = createTask(user, project.id(), "Draft launch brief", "Demo task assigned to you with a scheduled focus block.", 2);
        com.taskmind.backend.task.domain.model.Task t2 = createTask(user, project.id(), "Review sprint risks", "Sample collaborative planning task.", 3);
        com.taskmind.backend.task.domain.model.Task t3 = createTask(user, project.id(), "Publish weekly report", "Sample report and activity data source.", 1);
        Instant now = Instant.now();
        for (com.taskmind.backend.task.domain.model.Task task : List.of(t1, t2, t3)) {
            jdbc.update("insert into scheduled_blocks (id,user_id,task_id,starts_at,ends_at,status,rationale,version,created_at,updated_at) values (?,?,?,?,?,'SCHEDULED',?,0,?,?)", UUID.randomUUID(), user.userId(), task.id(), OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(1).plusHours(1), "Demo focus block generated during onboarding", now, now);
            jdbc.update("insert into activity_events (id,event_id,event_type,actor_user_id,entity_type,entity_id,project_id,occurred_at,payload,context,created_at) values (?,?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), UUID.randomUUID(), "DEMO_ACTIVITY", user.userId(), "task", task.id(), project.id(), now, "{\"demo\":true}", "{\"source\":\"onboarding\"}", now);
        }
        return project;
    }

    private com.taskmind.backend.task.domain.model.Task createTask(AuthenticatedUser user, UUID projectId, String title, String description, int priority) {
        return tasks.create(user, new CreateTaskCommand(user.userId(), projectId, title, description, TaskStatus.TODO, priority, null, 60, EnergyLevel.MEDIUM, TaskSource.MANUAL, BigDecimal.ONE));
    }

    private String uniqueKey(String prefix) { return normalize(prefix).replaceAll("[^A-Z]", "").substring(0, Math.min(4, normalize(prefix).length())) + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT).substring(3, 7); }
    private String normalize(String value) { return String.valueOf(value == null ? "" : value).trim().toUpperCase(Locale.ROOT); }
    private boolean demoResetAllowed() { return environment.acceptsProfiles(Profiles.of("local", "demo", "e2e", "test")); }

    public record OnboardingResult(boolean onboardingCompleted, UUID projectId) {}
    public record TemplateSummary(String key, String name, String description) {}
}
