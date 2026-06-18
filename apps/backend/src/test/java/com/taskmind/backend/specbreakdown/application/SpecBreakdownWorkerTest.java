package com.taskmind.backend.specbreakdown.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiRunStatus;
import com.taskmind.ai.contracts.audit.AiRunSummary;
import com.taskmind.ai.contracts.capability.CapabilitiesResponse;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.contracts.capability.CapabilityResponse;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import com.taskmind.backend.ai.NovaClient;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownDraft;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobStatus;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobType;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownStatus;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownDraftRepository;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownJobRepository;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpecBreakdownWorkerTest {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DRAFT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PROJECT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID RUN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private DraftRepo drafts;
    private JobRepo jobs;
    private Nova nova;
    private SpecBreakdownApplicationService service;
    private SpecBreakdownWorker worker;

    @BeforeEach
    void setUp() {
        drafts = new DraftRepo();
        jobs = new JobRepo();
        nova = new Nova();
        service = new SpecBreakdownApplicationService(drafts, jobs, nova, new ObjectMapper(), null, (u, t, p) -> {});
        worker = new SpecBreakdownWorker(service);
    }

    @Test
    void workerSuccessUpdatesDraftAndJobState() {
        SpecBreakdownProcessingJob job = service.startJob(user(), DRAFT_ID, SpecBreakdownJobType.BREAKDOWN);

        worker.processQueuedJobs();

        SpecBreakdownProcessingJob updated = jobs.findById(job.id()).orElseThrow();
        assertThat(updated.status()).isEqualTo(SpecBreakdownJobStatus.SUCCEEDED);
        assertThat(updated.novaRunId()).isEqualTo(RUN_ID);
        assertThat(updated.checkpoint()).contains("Generated epic");
        assertThat(drafts.draft.status()).isEqualTo(SpecBreakdownStatus.READY_FOR_REVIEW);
        assertThat(drafts.draft.candidateTree()).contains("Generated epic");
    }

    @Test
    void workerFailureRecordsErrorMessage() {
        nova.failure = new IllegalStateException("nova unavailable");
        SpecBreakdownProcessingJob job = service.startJob(user(), DRAFT_ID, SpecBreakdownJobType.OUTLINE);

        worker.processQueuedJobs();

        SpecBreakdownProcessingJob updated = jobs.findById(job.id()).orElseThrow();
        assertThat(updated.status()).isEqualTo(SpecBreakdownJobStatus.FAILED);
        assertThat(updated.errorMessage()).isEqualTo("nova unavailable");
        assertThat(drafts.draft.status()).isEqualTo(SpecBreakdownStatus.FAILED);
    }

    @Test
    void pauseCommandPreventsQueuedJobFromProcessing() {
        SpecBreakdownProcessingJob job = service.startJob(user(), DRAFT_ID, SpecBreakdownJobType.ENRICH);
        service.commandJob(user(), job.id(), "pause");

        worker.processQueuedJobs();

        SpecBreakdownProcessingJob updated = jobs.findById(job.id()).orElseThrow();
        assertThat(updated.status()).isEqualTo(SpecBreakdownJobStatus.PAUSED);
        assertThat(updated.paused()).isTrue();
        assertThat(nova.calls).isZero();
    }

    @Test
    void cancelCommandPreventsQueuedJobFromProcessing() {
        SpecBreakdownProcessingJob job = service.startJob(user(), DRAFT_ID, SpecBreakdownJobType.MERGE);
        service.commandJob(user(), job.id(), "cancel");

        worker.processQueuedJobs();

        SpecBreakdownProcessingJob updated = jobs.findById(job.id()).orElseThrow();
        assertThat(updated.status()).isEqualTo(SpecBreakdownJobStatus.CANCELED);
        assertThat(updated.requestedCancel()).isTrue();
        assertThat(nova.calls).isZero();
    }

    @Test
    void cancelCommandDuringProcessingStopsBeforeDraftReady() {
        SpecBreakdownProcessingJob job = service.startJob(user(), DRAFT_ID, SpecBreakdownJobType.SECTION);
        nova.beforeReturn = () -> service.commandJob(user(), job.id(), "cancel");

        worker.processQueuedJobs();

        SpecBreakdownProcessingJob updated = jobs.findById(job.id()).orElseThrow();
        assertThat(updated.status()).isEqualTo(SpecBreakdownJobStatus.CANCELED);
        assertThat(updated.checkpoint()).contains("Generated epic");
        assertThat(drafts.draft.status()).isEqualTo(SpecBreakdownStatus.PROCESSING);
    }

    @Test
    void resumeCommandRejectsTerminalJobs() {
        for (SpecBreakdownJobStatus terminalStatus : List.of(
                SpecBreakdownJobStatus.SUCCEEDED,
                SpecBreakdownJobStatus.FAILED,
                SpecBreakdownJobStatus.CANCELED)) {
            SpecBreakdownProcessingJob job = saveJobWithStatus(terminalStatus);

            assertThatThrownBy(() -> service.commandJob(user(), job.id(), "resume"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Spec breakdown job can only be resumed from PAUSED");

            assertThat(jobs.findById(job.id()).orElseThrow().status()).isEqualTo(terminalStatus);
        }
    }

    private SpecBreakdownProcessingJob saveJobWithStatus(SpecBreakdownJobStatus status) {
        Instant now = Instant.now();
        return jobs.save(
                new SpecBreakdownProcessingJob(
                        UUID.randomUUID(),
                        null,
                        DRAFT_ID,
                        USER_ID,
                        SpecBreakdownJobType.BREAKDOWN,
                        status,
                        "{}",
                        null,
                        null,
                        false,
                        false,
                        now,
                        now,
                        status == SpecBreakdownJobStatus.SUCCEEDED
                                        || status == SpecBreakdownJobStatus.FAILED
                                        || status == SpecBreakdownJobStatus.CANCELED
                                ? now
                                : null));
    }

    private AuthenticatedUser user() {
        return new AuthenticatedUser(USER_ID, Set.of("USER"));
    }

    static class DraftRepo implements SpecBreakdownDraftRepository {
        SpecBreakdownDraft draft = new SpecBreakdownDraft(DRAFT_ID, 0L, PROJECT_ID, USER_ID, null, "Spec", "Build it", null, "{\"nodes\":[]}", SpecBreakdownStatus.DRAFT, null, null, null, null, null, null, Instant.now(), Instant.now());

        public SpecBreakdownDraft save(SpecBreakdownDraft draft) {
            this.draft = draft;
            return draft;
        }

        public Optional<SpecBreakdownDraft> findById(UUID id) {
            return DRAFT_ID.equals(id) ? Optional.of(draft) : Optional.empty();
        }

        public List<SpecBreakdownDraft> findByProjectId(UUID projectId) {
            return List.of();
        }
    }

    static class JobRepo implements SpecBreakdownJobRepository {
        final List<SpecBreakdownProcessingJob> jobs = new ArrayList<>();

        public SpecBreakdownProcessingJob save(SpecBreakdownProcessingJob job) {
            jobs.removeIf(existing -> existing.id().equals(job.id()));
            SpecBreakdownProcessingJob persisted = new SpecBreakdownProcessingJob(job.id(), job.version() == null ? 0L : job.version() + 1, job.draftId(), job.userId(), job.aiJobType(), job.status(), job.checkpoint(), job.novaRunId(), job.errorMessage(), job.requestedCancel(), job.paused(), job.createdAt(), job.updatedAt(), job.completedAt());
            jobs.add(persisted);
            return persisted;
        }

        public Optional<SpecBreakdownProcessingJob> findById(UUID id) {
            return jobs.stream().filter(job -> job.id().equals(id)).findFirst();
        }

        public List<SpecBreakdownProcessingJob> findByDraftId(UUID draftId) {
            return jobs.stream().filter(job -> job.draftId().equals(draftId)).toList();
        }

        public Optional<SpecBreakdownProcessingJob> findFirstByStatusOrderByCreatedAt(SpecBreakdownJobStatus status) {
            return jobs.stream()
                    .filter(job -> job.status() == status)
                    .min(Comparator.comparing(SpecBreakdownProcessingJob::createdAt));
        }
    }

    class Nova implements NovaClient {
        int calls;
        RuntimeException failure;
        Runnable beforeReturn;

        public CapabilityResponse executeCapability(String capabilityId, CapabilityRequest request) {
            calls++;
            if (failure != null) {
                throw failure;
            }
            if (beforeReturn != null) {
                beforeReturn.run();
            }
            return new CapabilityResponse(RUN_ID, AiRunStatus.SUCCEEDED, new ObjectMapper().createObjectNode().putArray("nodes").addObject().put("title", "Generated epic"), List.of(), null);
        }

        public ChatResponse chat(ChatRequest request) { throw new UnsupportedOperationException(); }
        public void chatStream(ChatRequest request, OutputStream outputStream) throws IOException { throw new UnsupportedOperationException(); }
        public CapabilitiesResponse capabilities() { throw new UnsupportedOperationException(); }
        public AiRunSummary run(UUID runId) { throw new UnsupportedOperationException(); }
    }
}
