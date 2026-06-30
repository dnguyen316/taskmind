package com.taskmind.backend.task.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import com.taskmind.backend.tasktype.domain.repository.TaskTypeRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskTypeRepository taskTypes;

    @Autowired
    private ProjectRepository projects;

    @Test
    void createsTask() throws Exception {
        var payload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "title": "Write backend APIs",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        mockMvc.perform(post("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Write backend APIs"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.priority").value(2));
    }


    @Test
    void preservesDefaultSystemTaskTypeBehavior() throws Exception {
        var payload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "title": "Default type task",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        mockMvc.perform(post("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.taskLevel").value("TASK"))
            .andExpect(jsonPath("$.taskType").value("TASK"));
    }

    @Test
    void acceptsCustomTaskTypeForAllowedNormalTaskLevel() throws Exception {
        var owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var project = createProject(owner, "CT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        saveTaskType(project.id(), "CUSTOM_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(), true, TaskLevel.TASK, Set.of(TaskLevel.TASK));
        var type = taskTypes.findActive(project.id()).stream().filter(t -> t.key().startsWith("CUSTOM_")).findFirst().orElseThrow();
        var payload = """
            {
              "userId": "%s",
              "projectId": "%s",
              "title": "Custom type task",
              "taskType": "%s",
              "taskLevel": "TASK",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """.formatted(owner, project.id(), type.key());

        mockMvc.perform(post("/v1/tasks").with(jwt(owner.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.taskType").value(type.key()))
            .andExpect(jsonPath("$.taskLevel").value("TASK"));
    }

    @Test
    void rejectsCustomTaskTypeWhenLevelRulesDisallowIt() throws Exception {
        var owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var project = createProject(owner, "DS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        var type = "EPICONLY_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        saveTaskType(project.id(), type, true, TaskLevel.EPIC, Set.of(TaskLevel.EPIC));
        var payload = """
            {
              "userId": "%s",
              "projectId": "%s",
              "title": "Invalid custom type task",
              "taskType": "%s",
              "taskLevel": "TASK",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """.formatted(owner, project.id(), type);

        mockMvc.perform(post("/v1/tasks").with(jwt(owner.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInactiveTaskTypeForNewTask() throws Exception {
        var owner = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var project = createProject(owner, "IN" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        var type = "INACTIVE_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        saveTaskType(project.id(), type, false, TaskLevel.TASK, Set.of(TaskLevel.TASK));
        var payload = """
            {
              "userId": "%s",
              "projectId": "%s",
              "title": "Inactive custom type task",
              "taskType": "%s",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """.formatted(owner, project.id(), type);

        mockMvc.perform(post("/v1/tasks").with(jwt(owner.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isForbidden());
    }

    @Test
    void rejectsInvalidPriority() throws Exception {
        var payload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "title": "Invalid priority",
              "status": "TODO",
              "priority": 9,
              "source": "MANUAL"
            }
            """;

        mockMvc.perform(post("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updatesExistingTask() throws Exception {
        var createPayload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "title": "Task to patch",
              "status": "TODO",
              "priority": 3,
              "source": "MANUAL"
            }
            """;

        var createResponse = mockMvc.perform(post("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andReturn();

        var responseBody = createResponse.getResponse().getContentAsString();
        var id = objectMapper.readTree(responseBody).get("id").asText();

        var patchPayload = """
            {
              "status": "IN_PROGRESS",
              "priority": 1
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", id).with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.priority").value(1));
    }

    @Test
    void updatesTaskStatusAndChecksCompletion() throws Exception {
        var createPayload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "title": "Check completion",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        var createResponse = mockMvc.perform(post("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andReturn();

        var id = objectMapper.readTree(createResponse.getResponse().getContentAsString()).get("id").asText();

        var statusPayload = """
            {
              "status": "DONE"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}/status", id).with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(get("/v1/tasks/{id}/completion", id).with(jwt("11111111-1111-1111-1111-111111111111")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(true))
            .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void userCannotReadAnotherUsersTasksEvenWhenUserIdFilterIsProvided() throws Exception {
        var payloadA = """
            {
              "userId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "title": "User A task",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        var payloadB = """
            {
              "userId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "title": "User B task",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        mockMvc.perform(post("/v1/tasks").with(jwt("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")).contentType(MediaType.APPLICATION_JSON).content(payloadA))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/tasks").with(jwt("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")).contentType(MediaType.APPLICATION_JSON).content(payloadB))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/tasks").with(jwt("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")).queryParam("userId", "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));

        mockMvc.perform(get("/v1/tasks").with(jwt("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    }

    @Test
    void filtersByStatusAndPagination() throws Exception {
        var todoPayload = """
            {
              "userId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
              "title": "Todo task",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;
        var donePayload = """
            {
              "userId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
              "title": "Done task",
              "status": "DONE",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        mockMvc.perform(post("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111")).contentType(MediaType.APPLICATION_JSON).content(todoPayload))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111")).contentType(MediaType.APPLICATION_JSON).content(donePayload))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .queryParam("userId", "cccccccc-cccc-cccc-cccc-cccccccccccc")
                .queryParam("status", "TODO")
                .queryParam("page", "0")
                .queryParam("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    @Test
    void filtersOverdueAndArchivesTask() throws Exception {
        var overdueDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).toString();
        var createPayload = """
            {
              "userId": "dddddddd-dddd-dddd-dddd-dddddddddddd",
              "title": "Overdue task",
              "status": "TODO",
              "priority": 2,
              "dueAt": "%s",
              "source": "MANUAL"
            }
            """.formatted(overdueDate);

        var createResponse = mockMvc.perform(post("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andReturn();

        var id = objectMapper.readTree(createResponse.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .queryParam("userId", "dddddddd-dddd-dddd-dddd-dddddddddddd")
                .queryParam("overdueOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(patch("/v1/tasks/{id}/archive", id).with(jwt("11111111-1111-1111-1111-111111111111")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(get("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .queryParam("userId", "dddddddd-dddd-dddd-dddd-dddddddddddd")
                .queryParam("overdueOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsProjectTaskCreationForNonMember() throws Exception {
        var projectPayload = """
            {
              "name": "Membership protected project",
              "key": "MPP",
              "ownerUserId": "99999999-9999-9999-9999-999999999999"
            }
            """;

        var projectResponse = mockMvc.perform(post("/v1/projects").with(jwt("99999999-9999-9999-9999-999999999999"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(projectPayload))
            .andExpect(status().isCreated())
            .andReturn();

        var projectId = objectMapper.readTree(projectResponse.getResponse().getContentAsString()).get("id").asText();

        var taskPayload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "projectId": "%s",
              "title": "Forbidden project task",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """.formatted(projectId);

        mockMvc.perform(post("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskPayload))
            .andExpect(status().isForbidden());
    }

    @Test
    void allowsProjectTaskCreationForMember() throws Exception {
        var projectPayload = """
            {
              "name": "Membership allowed project",
              "key": "MAP",
              "ownerUserId": "99999999-9999-9999-9999-999999999998"
            }
            """;

        var projectResponse = mockMvc.perform(post("/v1/projects").with(jwt("99999999-9999-9999-9999-999999999998"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(projectPayload))
            .andExpect(status().isCreated())
            .andReturn();

        var projectId = objectMapper.readTree(projectResponse.getResponse().getContentAsString()).get("id").asText();

        var addMemberPayload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "role": "MEMBER"
            }
            """;

        mockMvc.perform(post("/v1/projects/{projectId}/members", projectId).with(jwt("99999999-9999-9999-9999-999999999998"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(addMemberPayload))
            .andExpect(status().isCreated());

        var taskPayload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "projectId": "%s",
              "title": "Allowed project task",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """.formatted(projectId);

        mockMvc.perform(post("/v1/tasks").with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(taskPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.projectId").value(projectId));
    }
    @Test
    void arbitraryIdentityAndRoleHeadersCannotImpersonateOrElevatePrivileges() throws Exception {
        var victimPayload = """
            {
              "userId": "ffffffff-ffff-ffff-ffff-ffffffffffff",
              "title": "Victim task",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        var victimResponse = mockMvc.perform(post("/v1/tasks")
                .with(jwt("ffffffff-ffff-ffff-ffff-ffffffffffff"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(victimPayload))
            .andExpect(status().isCreated())
            .andReturn();
        var victimTaskId = objectMapper.readTree(victimResponse.getResponse().getContentAsString()).get("id").asText();

        var attackerPayload = victimPayload.replace("Victim task", "Attempted impersonation");
        mockMvc.perform(post("/v1/tasks")
                .with(jwt("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"))
                .header("X-User-Id", "ffffffff-ffff-ffff-ffff-ffffffffffff")
                .header("X-User-Roles", "ADMIN,MANAGER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(attackerPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"));

        mockMvc.perform(patch("/v1/tasks/{id}/status", victimTaskId)
                .with(jwt("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"))
                .header("X-User-Id", "ffffffff-ffff-ffff-ffff-ffffffffffff")
                .header("X-User-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"))
            .andExpect(status().isForbidden());
    }


    @Test
    void rejectsStaleTaskUpdateWithConflict() throws Exception {
        var created = mockMvc.perform(post("/v1/tasks")
                .with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"11111111-1111-1111-1111-111111111111","title":"Versioned","status":"TODO","priority":2,"source":"MANUAL"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.version").isNumber())
            .andReturn();
        var id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/v1/tasks/{id}", id)
                .with(jwt("11111111-1111-1111-1111-111111111111")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").isNumber());

        mockMvc.perform(get("/v1/tasks")
                .with(jwt("11111111-1111-1111-1111-111111111111")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].version").isNumber());

        mockMvc.perform(patch("/v1/tasks/{id}", id)
                .with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":999,\"title\":\"stale\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void projectMembersCanCreateListAndDeleteTaskLinksButUnrelatedUsersCannot() throws Exception {
        var ownerId = "12121212-1212-1212-1212-121212121212";
        var taskOwnerId = "23232323-2323-2323-2323-232323232323";
        var memberId = "34343434-3434-3434-3434-343434343434";
        var unrelatedId = "45454545-4545-4545-4545-454545454545";

        var projectResponse = mockMvc.perform(post("/v1/projects").with(jwt(ownerId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Task link collaboration project",
                      "key": "LNK",
                      "ownerUserId": "%s"
                    }
                    """.formatted(ownerId)))
            .andExpect(status().isCreated())
            .andReturn();
        var projectId = objectMapper.readTree(projectResponse.getResponse().getContentAsString()).get("id").asText();

        addProjectMember(projectId, ownerId, taskOwnerId);
        addProjectMember(projectId, ownerId, memberId);

        var sourceTaskId = createProjectTask(projectId, taskOwnerId, "Link source");
        var targetTaskId = createProjectTask(projectId, taskOwnerId, "Link target");

        mockMvc.perform(post("/v1/tasks/{taskId}/links", sourceTaskId).with(jwt(unrelatedId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetTaskId":"%s","linkType":"BLOCKS"}
                    """.formatted(targetTaskId)))
            .andExpect(status().isForbidden());

        var linkResponse = mockMvc.perform(post("/v1/tasks/{taskId}/links", sourceTaskId).with(jwt(memberId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"targetTaskId":"%s","linkType":"BLOCKS"}
                    """.formatted(targetTaskId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sourceTaskId").value(sourceTaskId))
            .andExpect(jsonPath("$.targetTaskId").value(targetTaskId))
            .andExpect(jsonPath("$.linkType").value("BLOCKS"))
            .andReturn();
        var linkId = objectMapper.readTree(linkResponse.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/v1/tasks/{taskId}/links", sourceTaskId).with(jwt(memberId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(linkId));

        mockMvc.perform(get("/v1/tasks/{taskId}/links", sourceTaskId).with(jwt(unrelatedId)))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/v1/task-links/{id}", linkId).with(jwt(memberId)))
            .andExpect(status().isNoContent());
    }

    private void addProjectMember(String projectId, String ownerId, String memberId) throws Exception {
        mockMvc.perform(post("/v1/projects/{projectId}/members", projectId).with(jwt(ownerId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":"%s","role":"MEMBER"}
                    """.formatted(memberId)))
            .andExpect(status().isCreated());
    }

    private String createProjectTask(String projectId, String userId, String title) throws Exception {
        var response = mockMvc.perform(post("/v1/tasks").with(jwt(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "%s",
                      "projectId": "%s",
                      "title": "%s",
                      "status": "TODO",
                      "priority": 2,
                      "source": "MANUAL"
                    }
                    """.formatted(userId, projectId, title)))
            .andExpect(status().isCreated())
            .andReturn();
        return objectMapper.readTree(response.getResponse().getContentAsString()).get("id").asText();
    }


    private Project createProject(UUID owner, String key) {
        var now = Instant.now();
        return projects.save(new Project(UUID.randomUUID(), null, key + " Project", key, null, owner, null, now, now));
    }

    private void saveTaskType(UUID projectId, String key, boolean active, TaskLevel defaultLevel, Set<TaskLevel> allowedLevels) {
        var now = Instant.now();
        taskTypes.save(new TaskTypeDefinition(UUID.randomUUID(), null, projectId, key, key, null, null, defaultLevel, allowedLevels, false, false, null, false, active, 100, now, now));
    }
}
