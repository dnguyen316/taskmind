package com.taskmind.backend.task.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class PlanningControllerTest {

    private static final String REQUESTER_ID = "77777777-7777-7777-7777-777777777777";
    private static final String OTHER_USER_ID = "88888888-8888-8888-8888-888888888888";
    private static final String PRIVILEGED_TARGET_ID = "99999999-8888-8888-8888-888888888888";
    private static final String TODO_PLANNER_USER_ID = "70707070-7070-7070-7070-707070707070";
    private static final String BLOCKED_PLANNER_USER_ID = "71717171-7171-7171-7171-717171717171";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void captureFallsBackToDeterministicDraftsWhenNovaUnavailable() throws Exception {
        Integer beforeCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where event_type='ai.capture_submitted'",
                        Integer.class);

        mockMvc.perform(post("/v1/ai/capture")
                .with(jwt(REQUESTER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "text": "Draft launch memo\\nBook design review"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.drafts[0].title").value("Draft launch memo"))
            .andExpect(jsonPath("$.drafts[1].source").value("AI_CAPTURE"));

        Integer afterCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where event_type='ai.capture_submitted'",
                        Integer.class);
        String payload =
                jdbcTemplate.queryForObject(
                        "select payload from outbox_events where event_type='ai.capture_submitted' order by created_at desc limit 1",
                        String.class);
        org.assertj.core.api.Assertions.assertThat(afterCount).isEqualTo(beforeCount + 1);
        org.assertj.core.api.Assertions.assertThat(payload).contains("ai.capture_submitted").contains("length");
    }


    @Test
    void acceptCapturedDraftCreatesRequesterScopedTaskAndPublishesEvent() throws Exception {
        Integer beforeCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where event_type='ai.suggestion_accepted'",
                        Integer.class);

        var response = mockMvc.perform(post("/v1/ai/capture/accept")
                .with(jwt(REQUESTER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "draft": {
                        "title": "Accept captured task",
                        "status": "TODO",
                        "priority": 2,
                        "durationMinutes": 45,
                        "confidence": 0.86
                      },
                      "description": "Created from the capture panel"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taskId").exists())
            .andReturn();

        String taskId = objectMapper.readTree(response.getResponse().getContentAsString()).get("taskId").asText();
        String ownerUserId =
                jdbcTemplate.queryForObject("select user_id from tasks where id = ?", String.class, java.util.UUID.fromString(taskId));
        Integer afterCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where event_type='ai.suggestion_accepted'",
                        Integer.class);
        String payload =
                jdbcTemplate.queryForObject(
                        "select payload from outbox_events where event_type='ai.suggestion_accepted' order by created_at desc limit 1",
                        String.class);

        org.assertj.core.api.Assertions.assertThat(ownerUserId).isEqualTo(REQUESTER_ID);
        org.assertj.core.api.Assertions.assertThat(afterCount).isEqualTo(beforeCount + 1);
        org.assertj.core.api.Assertions.assertThat(payload)
                .contains("ai.suggestion_accepted")
                .contains(taskId)
                .contains("Accept captured task");
    }

    @Test
    void managerAcceptCapturedDraftStillCreatesTaskForAuthenticatedRequester() throws Exception {
        var response = mockMvc.perform(post("/v1/ai/capture/accept")
                .with(jwt(REQUESTER_ID, "MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "draft": {
                        "title": "Manager scoped capture",
                        "status": "TODO",
                        "priority": 3,
                        "durationMinutes": 30,
                        "confidence": 0.75
                      }
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        String taskId = objectMapper.readTree(response.getResponse().getContentAsString()).get("taskId").asText();
        String ownerUserId =
                jdbcTemplate.queryForObject("select user_id from tasks where id = ?", String.class, java.util.UUID.fromString(taskId));

        org.assertj.core.api.Assertions.assertThat(ownerUserId).isEqualTo(REQUESTER_ID);
    }

    @Test
    void rejectCapturedDraftPublishesRejectedEvent() throws Exception {
        Integer beforeCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where event_type='ai.suggestion_rejected'",
                        Integer.class);

        mockMvc.perform(post("/v1/ai/capture/reject")
                .with(jwt(REQUESTER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "draft": {
                        "title": "Reject captured task",
                        "status": "TODO",
                        "priority": 2,
                        "durationMinutes": 20,
                        "confidence": 0.4
                      },
                      "reason": "Duplicate"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rejected").value(true));

        Integer afterCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from outbox_events where event_type='ai.suggestion_rejected'",
                        Integer.class);
        String payload =
                jdbcTemplate.queryForObject(
                        "select payload from outbox_events where event_type='ai.suggestion_rejected' order by created_at desc limit 1",
                        String.class);

        org.assertj.core.api.Assertions.assertThat(afterCount).isEqualTo(beforeCount + 1);
        org.assertj.core.api.Assertions.assertThat(payload)
                .contains("ai.suggestion_rejected")
                .contains("Reject captured task")
                .contains("Duplicate");
    }

    @Test
    void translateTaskFallsBackToDeterministicTranslationWhenNovaUnavailable() throws Exception {
        mockMvc.perform(post("/v1/ai/tasks/translate")
                .with(jwt(REQUESTER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "text": "Write release notes",
                      "targetLanguage": "French"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.translatedText").value("[French] Write release notes"))
            .andExpect(jsonPath("$.targetLanguage").value("French"));
    }

    @Test
    void arbitraryRoleHeaderCannotElevateDailyPlannerAccess() throws Exception {
        createTask(OTHER_USER_ID, "Other user's task");

        mockMvc.perform(post("/v1/planner/daily/generate")
                .with(jwt(REQUESTER_ID))
                .header("X-User-Id", OTHER_USER_ID)
                .header("X-User-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "88888888-8888-8888-8888-888888888888",
                      "availableMinutes": 60,
                      "includeBlockedTasks": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan.length()").value(0))
            .andExpect(jsonPath("$.overflow.length()").value(0));
    }

    @Test
    void validatedJwtRoleAllowsPrivilegedDailyPlannerAccess() throws Exception {
        createTask(PRIVILEGED_TARGET_ID, "Other user's privileged task");

        mockMvc.perform(post("/v1/planner/daily/generate")
                .with(jwt(REQUESTER_ID, "MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "99999999-8888-8888-8888-888888888888",
                      "availableMinutes": 60,
                      "includeBlockedTasks": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan[0].title").value("Other user's privileged task"));
    }

    @Test
    void dailyPlannerIncludesTodoTasksWhenBlockedTasksExcluded() throws Exception {
        createTask(TODO_PLANNER_USER_ID, "Default TODO planner task", "TODO");

        mockMvc.perform(post("/v1/planner/daily/generate")
                .with(jwt(TODO_PLANNER_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "70707070-7070-7070-7070-707070707070",
                      "availableMinutes": 60,
                      "includeBlockedTasks": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan[0].title").value("Default TODO planner task"))
            .andExpect(jsonPath("$.plan[0].status").value("TODO"));
    }

    @Test
    void dailyPlannerExcludesBlockedTasksByDefaultAndIncludesThemWhenRequested() throws Exception {
        String projectId = createProject(BLOCKED_PLANNER_USER_ID);
        String dependencyId = createTask(BLOCKED_PLANNER_USER_ID, "Blocking dependency", "IN_PROGRESS", projectId);
        String blockedTaskId = createTask(BLOCKED_PLANNER_USER_ID, "Blocked target task", "TODO", projectId);
        createTaskLink(BLOCKED_PLANNER_USER_ID, dependencyId, blockedTaskId, "BLOCKS");

        mockMvc.perform(post("/v1/planner/daily/generate")
                .with(jwt(BLOCKED_PLANNER_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "71717171-7171-7171-7171-717171717171",
                      "availableMinutes": 120,
                      "includeBlockedTasks": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan.length()").value(1))
            .andExpect(jsonPath("$.plan[0].title").value("Blocking dependency"));

        mockMvc.perform(post("/v1/planner/daily/generate")
                .with(jwt(BLOCKED_PLANNER_USER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "71717171-7171-7171-7171-717171717171",
                      "availableMinutes": 120,
                      "includeBlockedTasks": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan.length()").value(2))
            .andExpect(jsonPath("$.plan[0].title").value("Blocked target task"));
    }

    @Test
    void weeklyReviewUsesAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(post("/v1/review/weekly/generate")
                .with(jwt(REQUESTER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(REQUESTER_ID))
            .andExpect(jsonPath("$.summary").exists());
    }

    @Test
    void weeklyReviewIgnoresCrossUserRequestBodyId() throws Exception {
        mockMvc.perform(post("/v1/review/weekly/generate")
                .with(jwt(REQUESTER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "88888888-8888-8888-8888-888888888888"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(REQUESTER_ID));
    }

    private String createTask(String userId, String title) throws Exception {
        return createTask(userId, title, "IN_PROGRESS");
    }

    private String createTask(String userId, String title, String status) throws Exception {
        return createTask(userId, title, status, null);
    }

    private String createTask(String userId, String title, String status, String projectId) throws Exception {
        String projectField = projectId == null ? "" : "\n                      \"projectId\": \"" + projectId + "\",";
        var response = mockMvc.perform(post("/v1/tasks")
                .with(jwt(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "%s",%s
                      "title": "%s",
                      "status": "%s",
                      "priority": 2,
                      "durationMinutes": 30,
                      "source": "MANUAL"
                    }
                    """.formatted(userId, projectField, title, status)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(response.getResponse().getContentAsString()).get("id").asText();
    }

    private String createProject(String ownerUserId) throws Exception {
        var response = mockMvc.perform(post("/v1/projects")
                .with(jwt(ownerUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Planning dependency project",
                      "key": "%s",
                      "ownerUserId": "%s"
                    }
                    """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase(), ownerUserId)))
            .andExpect(status().isCreated())
            .andReturn();
        String projectId = objectMapper.readTree(response.getResponse().getContentAsString()).get("id").asText();
        addProjectMember(ownerUserId, projectId, ownerUserId, "OWNER");
        return projectId;
    }

    private void addProjectMember(String actorUserId, String projectId, String userId, String role) throws Exception {
        mockMvc.perform(post("/v1/projects/{projectId}/members", projectId)
                .with(jwt(actorUserId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "%s",
                      "role": "%s"
                    }
                    """.formatted(userId, role)))
            .andExpect(status().isCreated());
    }

    private void createTaskLink(String userId, String sourceTaskId, String targetTaskId, String linkType) throws Exception {
        mockMvc.perform(post("/v1/tasks/{taskId}/links", sourceTaskId)
                .with(jwt(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "targetTaskId": "%s",
                      "linkType": "%s"
                    }
                    """.formatted(targetTaskId, linkType)))
            .andExpect(status().isCreated());
    }
}
