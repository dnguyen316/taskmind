package com.taskmind.backend.specbreakdown.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.security.*;
import com.taskmind.backend.specbreakdown.application.*;
import com.taskmind.backend.specbreakdown.domain.model.*;
import com.taskmind.backend.specbreakdown.domain.repository.*;
import com.taskmind.backend.task.application.CreateTaskCommand;
import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.domain.model.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SpecBreakdownController.class)
@Import({SecurityConfig.class, AuthenticatedUserResolver.class, JwtClaimAuthenticationConverter.class, TestJwtSupport.Config.class, SpecBreakdownControllerTest.Fakes.class})
class SpecBreakdownControllerTest {
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID DRAFT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROJECT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired MockMvc mockMvc;
    @Autowired DraftRepo drafts;
    @Autowired CapturingTaskService taskService;

    @BeforeEach
    void resetFakes() {
        drafts.reset();
        taskService.reset();
    }

    @Test
    void createsDraftBehindAuthenticatedCoreFacade() throws Exception {
        mockMvc.perform(post("/v1/spec-breakdown/drafts")
                        .with(jwt(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {"projectId":"33333333-3333-3333-3333-333333333333","title":"Mobile spec","rawSpec":"Build mobile onboarding"}
            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(DRAFT_ID.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void startsJobAndReturnsQueuedAcceptedStatusEnvelope() throws Exception {
        mockMvc.perform(post("/v1/spec-breakdown/drafts/{id}/jobs", DRAFT_ID)
                        .with(jwt(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aiJobType\":\"OUTLINE\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.aiJobType").value("OUTLINE"))
                .andExpect(jsonPath("$.status").value("QUEUED"));
    }

    @Test
    void returnsJobStatus() throws Exception {
        mockMvc.perform(get("/v1/spec-breakdown/jobs/{id}", Fakes.JOB_ID).with(jwt(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void materializesNestedTreeWithGeneratedParentChildRelationships() throws Exception {
        drafts.replaceCandidateTree("""
                {"nodes":[{"level":"EPIC","title":"Parent epic","description":"Epic details","storyPoints":13,"children":[{"level":"STORY","title":"Child story","storyPoints":5,"children":[{"level":"SUBTASK","title":"Grandchild task"}]}]}]}
                """);

        mockMvc.perform(post("/v1/spec-breakdown/drafts/{id}/materialize", DRAFT_ID).with(jwt(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskIds[0]").value(CapturingTaskService.idFor(0).toString()));

        assertThat(taskService.commands()).hasSize(3);
        CreateTaskCommand epic = taskService.commands().get(0);
        CreateTaskCommand story = taskService.commands().get(1);
        CreateTaskCommand task = taskService.commands().get(2);

        assertThat(epic.parentTaskId()).isNull();
        assertThat(epic.taskLevel()).isEqualTo(TaskLevel.EPIC);
        assertThat(epic.taskType()).isEqualTo("EPIC");
        assertThat(epic.storyPoints()).isEqualTo(13);
        assertThat(epic.title()).isEqualTo("Parent epic");
        assertThat(epic.description()).isEqualTo("Epic details");

        assertThat(story.parentTaskId()).isEqualTo(CapturingTaskService.idFor(0));
        assertThat(story.taskLevel()).isEqualTo(TaskLevel.STORY);
        assertThat(story.taskType()).isEqualTo("STORY");
        assertThat(story.storyPoints()).isEqualTo(5);

        assertThat(task.parentTaskId()).isEqualTo(CapturingTaskService.idFor(1));
        assertThat(task.taskLevel()).isEqualTo(TaskLevel.SUBTASK);
        assertThat(task.taskType()).isEqualTo("SUBTASK");
        assertThat(task.storyPoints()).isNull();
    }

    @Test
    void materializeDefaultsRootAndChildLevelTypeWhenFieldsAreMissing() throws Exception {
        drafts.replaceCandidateTree("""
                {"nodes":[{"children":[{}]}]}
                """);

        mockMvc.perform(post("/v1/spec-breakdown/drafts/{id}/materialize", DRAFT_ID).with(jwt(USER_ID)))
                .andExpect(status().isOk());

        assertThat(taskService.commands()).hasSize(2);
        CreateTaskCommand root = taskService.commands().get(0);
        CreateTaskCommand child = taskService.commands().get(1);

        assertThat(root.taskLevel()).isEqualTo(TaskLevel.EPIC);
        assertThat(root.taskType()).isEqualTo("EPIC");
        assertThat(root.title()).isEqualTo("Generated task");
        assertThat(root.description()).isNull();
        assertThat(root.storyPoints()).isNull();

        assertThat(child.parentTaskId()).isEqualTo(CapturingTaskService.idFor(0));
        assertThat(child.taskLevel()).isEqualTo(TaskLevel.SUBTASK);
        assertThat(child.taskType()).isEqualTo("SUBTASK");
        assertThat(child.title()).isEqualTo("Generated task");
        assertThat(child.description()).isNull();
        assertThat(child.storyPoints()).isNull();
    }

    @TestConfiguration
    static class Fakes {
        static final UUID JOB_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

        @Bean
        DraftRepo draftRepo() {
            return new DraftRepo();
        }

        @Bean
        JobRepo jobRepo() {
            return new JobRepo();
        }

        @Bean
        CapturingTaskService capturingTaskService() {
            return new CapturingTaskService();
        }

        @Bean
        SpecBreakdownApplicationService specBreakdownApplicationService(
                DraftRepo draftRepo, JobRepo jobRepo, CapturingTaskService taskService) {
            return new SpecBreakdownApplicationService(draftRepo, jobRepo, null, new ObjectMapper(), taskService, (u, t, p) -> {}) {
                @Override
                public SpecBreakdownDraft createDraft(AuthenticatedUser u, CreateDraftCommand c) {
                    return draftRepo.draft();
                }

                @Override
                public SpecBreakdownProcessingJob startJob(AuthenticatedUser u, UUID draftId, SpecBreakdownJobType type) {
                    return new SpecBreakdownProcessingJob(JOB_ID, 0L, draftId, u.userId(), type, SpecBreakdownJobStatus.QUEUED, "{}", null, null, false, false, Instant.now(), Instant.now(), null);
                }

                @Override
                public Optional<SpecBreakdownProcessingJob> getJob(AuthenticatedUser u, UUID id) {
                    return Optional.of(new SpecBreakdownProcessingJob(id, 0L, DRAFT_ID, u.userId(), SpecBreakdownJobType.BREAKDOWN, SpecBreakdownJobStatus.PAUSED, "{}", null, null, false, true, Instant.now(), Instant.now(), null));
                }
            };
        }
    }

    static class DraftRepo implements SpecBreakdownDraftRepository {
        private SpecBreakdownDraft draft = defaultDraft();

        void reset() {
            draft = defaultDraft();
        }

        SpecBreakdownDraft draft() {
            return draft;
        }

        void replaceCandidateTree(String candidateTree) {
            draft = new SpecBreakdownDraft(draft.id(), draft.version(), draft.projectId(), draft.ownerUserId(), draft.templateId(), draft.title(), draft.rawSpec(), draft.richContent(), candidateTree, draft.status(), draft.fixVersion(), draft.affectedVersion(), draft.sprint(), draft.issueType(), draft.publishKey(), draft.materializedAt(), draft.createdAt(), draft.updatedAt());
        }

        public SpecBreakdownDraft save(SpecBreakdownDraft d) {
            draft = d;
            return d;
        }

        public Optional<SpecBreakdownDraft> findById(UUID id) {
            return DRAFT_ID.equals(id) ? Optional.of(draft) : Optional.empty();
        }

        public List<SpecBreakdownDraft> findByProjectId(UUID projectId) {
            return List.of();
        }

        private static SpecBreakdownDraft defaultDraft() {
            Instant now = Instant.now();
            return new SpecBreakdownDraft(DRAFT_ID, 0L, PROJECT_ID, UUID.fromString(USER_ID), null, "Mobile spec", "Build mobile onboarding", null, "{\"nodes\":[]}", SpecBreakdownStatus.DRAFT, "1.0", null, null, null, null, null, now, now);
        }
    }

    static class JobRepo implements SpecBreakdownJobRepository {
        public SpecBreakdownProcessingJob save(SpecBreakdownProcessingJob j) {
            return j;
        }

        public Optional<SpecBreakdownProcessingJob> findById(UUID id) {
            return Optional.empty();
        }

        public List<SpecBreakdownProcessingJob> findByDraftId(UUID draftId) {
            return List.of();
        }

        public Optional<SpecBreakdownProcessingJob> findFirstByStatusOrderByCreatedAt(SpecBreakdownJobStatus status) {
            return Optional.empty();
        }
    }

    static class CapturingTaskService extends TaskApplicationService {
        private final List<CreateTaskCommand> commands = new ArrayList<>();

        CapturingTaskService() {
            super(null, null, null, null);
        }

        void reset() {
            commands.clear();
        }

        List<CreateTaskCommand> commands() {
            return commands;
        }

        static UUID idFor(int index) {
            return UUID.fromString("00000000-0000-0000-0000-" + String.format("%012d", index + 1));
        }

        @Override
        public Task create(AuthenticatedUser requester, CreateTaskCommand c) {
            UUID id = idFor(commands.size());
            commands.add(c);
            Instant now = Instant.now();
            return new Task(id, null, c.userId(), c.projectId(), "TM-" + commands.size(), c.assigneeId(), c.parentTaskId(), c.taskLevel(), c.taskType(), c.storyPoints(), c.releaseVersion(), null, c.title(), c.description(), c.status(), c.priority(), c.dueAt(), c.durationMinutes(), c.energyLevel(), c.source(), BigDecimal.valueOf(0.80), now, now);
        }
    }
}
