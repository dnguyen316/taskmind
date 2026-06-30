package com.taskmind.backend.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.application.CreateProjectCommand;
import com.taskmind.backend.project.application.ProjectApplicationService;
import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import com.taskmind.backend.project.domain.repository.ProjectMembershipRepository;
import com.taskmind.backend.task.application.CreateTaskCommand;
import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TaskOutboxActivityIntegrationTest {
    @Autowired ProjectApplicationService projects;
    @Autowired ProjectMembershipRepository memberships;
    @Autowired TaskApplicationService tasks;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ObjectMapper objectMapper;

    @Test
    void creatingTaskWritesActivityAndOutboxRowsInSameTransaction() throws Exception {
        UUID userId = UUID.randomUUID();
        Project project = projects.create(new CreateProjectCommand("Events", "EVT", "", userId));
        memberships.save(new ProjectMembership(project.id(), userId, ProjectMembershipRole.OWNER));

        Integer beforeTaskOutbox =
                jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where event_type='task.created'",
                        Integer.class);
        Integer beforeTaskActivity =
                jdbcTemplate.queryForObject(
                        "select count(*) from activity_events where event_type='TASK_CREATED'",
                        Integer.class);

        tasks.create(
                new AuthenticatedUser(userId, Set.of("USER")),
                new CreateTaskCommand(
                        userId,
                        project.id(),
                        "Emit event",
                        "",
                        TaskStatus.TODO,
                        1,
                        null,
                        30,
                        null,
                        TaskSource.MANUAL,
                        null));

        Integer taskOutboxCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where event_type='task.created'",
                        Integer.class);
        Integer taskActivityCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from activity_events where event_type='TASK_CREATED'",
                        Integer.class);
        assertEquals(beforeTaskOutbox + 1, taskOutboxCount);
        assertEquals(beforeTaskActivity + 1, taskActivityCount);
        String payload =
                jdbcTemplate.queryForObject(
                        "select payload from outbox_events where event_type=\'task.created\' order by created_at desc limit 1",
                        String.class);
        assertEquals("TASK", objectMapper.readTree(payload).path("payload").path("taskTypeKey").asText());
        Integer projectOutboxCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where event_type='project.created'",
                        Integer.class);
        assertTrue(projectOutboxCount >= 1);
    }
}
