package com.taskmind.backend.specbreakdown.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.contracts.capability.CapabilityResponse;
import com.taskmind.backend.ai.NovaClient;
import com.taskmind.backend.ai.application.AiDomainEventPublisher;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownDraft;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobStatus;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobType;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownStatus;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownDraftRepository;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownJobRepository;
import com.taskmind.backend.task.application.CreateTaskCommand;
import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.events.EventTypes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class SpecBreakdownApplicationService {
    private static final BigDecimal GENERATED_TASK_CONFIDENCE = BigDecimal.valueOf(0.80);
    private static final int GENERATED_TASK_PRIORITY = 3;

    private final SpecBreakdownDraftRepository drafts;
    private final SpecBreakdownJobRepository jobs;
    private final NovaClient nova;
    private final ObjectMapper mapper;
    private final TaskApplicationService tasks;
    private final AiDomainEventPublisher events;
    private final TransactionTemplate transactions;

    @Autowired
    public SpecBreakdownApplicationService(
            SpecBreakdownDraftRepository drafts,
            SpecBreakdownJobRepository jobs,
            NovaClient nova,
            ObjectMapper mapper,
            TaskApplicationService tasks,
            AiDomainEventPublisher events,
            PlatformTransactionManager transactionManager) {
        this.drafts = drafts;
        this.jobs = jobs;
        this.nova = nova;
        this.mapper = mapper;
        this.tasks = tasks;
        this.events = events;
        this.transactions = transactionManager == null ? null : new TransactionTemplate(transactionManager);
        if (this.transactions != null) {
            this.transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            this.transactions.setReadOnly(false);
        }
    }

    public SpecBreakdownApplicationService(
            SpecBreakdownDraftRepository drafts,
            SpecBreakdownJobRepository jobs,
            NovaClient nova,
            ObjectMapper mapper,
            TaskApplicationService tasks,
            AiDomainEventPublisher events) {
        this(drafts, jobs, nova, mapper, tasks, events, null);
    }

    @Transactional
    public SpecBreakdownDraft createDraft(AuthenticatedUser authenticatedUser, CreateDraftCommand command) {
        Instant now = Instant.now();
        String tree = command.candidateTree() != null ? command.candidateTree() : "{\"nodes\":[]}";
        return drafts.save(
                new SpecBreakdownDraft(
                        UUID.randomUUID(),
                        null,
                        command.projectId(),
                        authenticatedUser.userId(),
                        command.templateId(),
                        command.title().trim(),
                        command.rawSpec(),
                        command.richContent(),
                        tree,
                        SpecBreakdownStatus.DRAFT,
                        command.fixVersion(),
                        command.affectedVersion(),
                        command.sprint(),
                        command.issueType(),
                        command.publishKey(),
                        null,
                        now,
                        now));
    }

    public Optional<SpecBreakdownDraft> getDraft(AuthenticatedUser authenticatedUser, UUID draftId) {
        return drafts.findById(draftId)
                .filter(draft -> authenticatedUser.isPrivileged() || draft.ownerUserId().equals(authenticatedUser.userId()));
    }

    @Transactional
    public SpecBreakdownProcessingJob startJob(
            AuthenticatedUser authenticatedUser, UUID draftId, SpecBreakdownJobType type) {
        SpecBreakdownDraft draft = require(authenticatedUser, draftId);
        Instant now = Instant.now();
        return jobs.save(
                new SpecBreakdownProcessingJob(
                        UUID.randomUUID(),
                        null,
                        draft.id(),
                        authenticatedUser.userId(),
                        type,
                        SpecBreakdownJobStatus.QUEUED,
                        "{}",
                        null,
                        null,
                        false,
                        false,
                        now,
                        now,
                        null));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean processOneQueuedJob() {
        ClaimedQueuedJob claimed = runInTransaction(this::claimNextQueuedJob);
        if (!claimed.queuedJobFound()) {
            return false;
        }
        if (claimed.jobToRun() == null) {
            return true;
        }

        SpecBreakdownProcessingJob running = claimed.jobToRun();
        SpecBreakdownDraft draft = drafts.findById(running.draftId()).orElseThrow();
        try {
            ObjectNode input = mapper.createObjectNode();
            input.put("draftId", draft.id().toString());
            input.put("rawSpec", draft.rawSpec());
            input.set("candidateTree", mapper.readTree(draft.candidateTree()));
            CapabilityResponse response =
                    nova.executeCapability(
                            capability(running.aiJobType()),
                            new CapabilityRequest(
                                    new AiCapabilityId(capability(running.aiJobType())),
                                    running.userId(),
                                    draft.projectId().toString(),
                                    input,
                                    running.id().toString(),
                                    running.id().toString()));
            String output =
                    response.output() == null
                            ? draft.candidateTree()
                            : mapper.writeValueAsString(response.output());
            runInTransaction(() -> completeSucceededJob(draft, running, output, response.runId()));
        } catch (Exception e) {
            runInTransaction(() -> completeFailedJob(draft, running, e));
        }
        return true;
    }

    private ClaimedQueuedJob claimNextQueuedJob() {
        Optional<SpecBreakdownProcessingJob> next =
                jobs.findFirstByStatusOrderByCreatedAt(SpecBreakdownJobStatus.QUEUED);
        if (next.isEmpty()) {
            return ClaimedQueuedJob.none();
        }
        SpecBreakdownProcessingJob job = next.get();
        if (job.requestedCancel()) {
            jobs.save(
                    new SpecBreakdownProcessingJob(
                            job.id(),
                            job.version(),
                            job.draftId(),
                            job.userId(),
                            job.aiJobType(),
                            SpecBreakdownJobStatus.CANCELED,
                            job.checkpoint(),
                            job.novaRunId(),
                            job.errorMessage(),
                            true,
                            false,
                            job.createdAt(),
                            Instant.now(),
                            Instant.now()));
            return ClaimedQueuedJob.terminallyHandled();
        }
        if (job.paused()) {
            jobs.save(
                    new SpecBreakdownProcessingJob(
                            job.id(),
                            job.version(),
                            job.draftId(),
                            job.userId(),
                            job.aiJobType(),
                            SpecBreakdownJobStatus.PAUSED,
                            job.checkpoint(),
                            job.novaRunId(),
                            job.errorMessage(),
                            false,
                            true,
                            job.createdAt(),
                            Instant.now(),
                            job.completedAt()));
            return ClaimedQueuedJob.terminallyHandled();
        }
        SpecBreakdownDraft draft = drafts.findById(job.draftId()).orElseThrow();
        drafts.save(
                new SpecBreakdownDraft(
                        draft.id(),
                        draft.version(),
                        draft.projectId(),
                        draft.ownerUserId(),
                        draft.templateId(),
                        draft.title(),
                        draft.rawSpec(),
                        draft.richContent(),
                        draft.candidateTree(),
                        SpecBreakdownStatus.PROCESSING,
                        draft.fixVersion(),
                        draft.affectedVersion(),
                        draft.sprint(),
                        draft.issueType(),
                        draft.publishKey(),
                        draft.materializedAt(),
                        draft.createdAt(),
                        Instant.now()));
        return ClaimedQueuedJob.running(
                jobs.save(
                        new SpecBreakdownProcessingJob(
                                job.id(),
                                job.version(),
                                job.draftId(),
                                job.userId(),
                                job.aiJobType(),
                                SpecBreakdownJobStatus.RUNNING,
                                job.checkpoint(),
                                null,
                                null,
                                false,
                                false,
                                job.createdAt(),
                                Instant.now(),
                                null)));
    }

    private record ClaimedQueuedJob(boolean queuedJobFound, SpecBreakdownProcessingJob jobToRun) {
        static ClaimedQueuedJob none() {
            return new ClaimedQueuedJob(false, null);
        }

        static ClaimedQueuedJob terminallyHandled() {
            return new ClaimedQueuedJob(true, null);
        }

        static ClaimedQueuedJob running(SpecBreakdownProcessingJob job) {
            return new ClaimedQueuedJob(true, job);
        }
    }

    private void completeSucceededJob(
            SpecBreakdownDraft draft, SpecBreakdownProcessingJob running, String output, UUID runId) {
        SpecBreakdownProcessingJob latest = jobs.findById(running.id()).orElse(running);
        if (latest.requestedCancel()) {
            jobs.save(
                    new SpecBreakdownProcessingJob(
                            latest.id(),
                            latest.version(),
                            latest.draftId(),
                            latest.userId(),
                            latest.aiJobType(),
                            SpecBreakdownJobStatus.CANCELED,
                            output,
                            runId,
                            latest.errorMessage(),
                            true,
                            false,
                            latest.createdAt(),
                            Instant.now(),
                            Instant.now()));
            return;
        }
        if (latest.paused()) {
            jobs.save(
                    new SpecBreakdownProcessingJob(
                            latest.id(),
                            latest.version(),
                            latest.draftId(),
                            latest.userId(),
                            latest.aiJobType(),
                            SpecBreakdownJobStatus.PAUSED,
                            output,
                            runId,
                            latest.errorMessage(),
                            false,
                            true,
                            latest.createdAt(),
                            Instant.now(),
                            latest.completedAt()));
            return;
        }
        drafts.save(
                new SpecBreakdownDraft(
                        draft.id(),
                        draft.version(),
                        draft.projectId(),
                        draft.ownerUserId(),
                        draft.templateId(),
                        draft.title(),
                        draft.rawSpec(),
                        draft.richContent(),
                        output,
                        SpecBreakdownStatus.READY_FOR_REVIEW,
                        draft.fixVersion(),
                        draft.affectedVersion(),
                        draft.sprint(),
                        draft.issueType(),
                        draft.publishKey(),
                        draft.materializedAt(),
                        draft.createdAt(),
                        Instant.now()));
        events.publish(
                running.userId(),
                EventTypes.AI_SPEC_BREAKDOWN_COMPLETED,
                Map.of(
                        "draftId",
                        draft.id().toString(),
                        "jobId",
                        running.id().toString(),
                        "jobType",
                        running.aiJobType().name()));
        jobs.save(
                new SpecBreakdownProcessingJob(
                        latest.id(),
                        latest.version(),
                        latest.draftId(),
                        latest.userId(),
                        latest.aiJobType(),
                        SpecBreakdownJobStatus.SUCCEEDED,
                        output,
                        runId,
                        null,
                        false,
                        false,
                        latest.createdAt(),
                        Instant.now(),
                        Instant.now()));
    }

    private void completeFailedJob(
            SpecBreakdownDraft draft, SpecBreakdownProcessingJob running, Exception e) {
        String message = e.getMessage() == null ? "failed" : e.getMessage();
        events.publish(
                running.userId(),
                EventTypes.AI_SPEC_BREAKDOWN_FAILED,
                Map.of(
                        "draftId",
                        draft.id().toString(),
                        "jobId",
                        running.id().toString(),
                        "message",
                        message));
        drafts.save(
                new SpecBreakdownDraft(
                        draft.id(),
                        draft.version(),
                        draft.projectId(),
                        draft.ownerUserId(),
                        draft.templateId(),
                        draft.title(),
                        draft.rawSpec(),
                        draft.richContent(),
                        draft.candidateTree(),
                        SpecBreakdownStatus.FAILED,
                        draft.fixVersion(),
                        draft.affectedVersion(),
                        draft.sprint(),
                        draft.issueType(),
                        draft.publishKey(),
                        draft.materializedAt(),
                        draft.createdAt(),
                        Instant.now()));
        SpecBreakdownProcessingJob latest = jobs.findById(running.id()).orElse(running);
        jobs.save(
                new SpecBreakdownProcessingJob(
                        latest.id(),
                        latest.version(),
                        latest.draftId(),
                        latest.userId(),
                        latest.aiJobType(),
                        latest.requestedCancel()
                                ? SpecBreakdownJobStatus.CANCELED
                                : SpecBreakdownJobStatus.FAILED,
                        latest.checkpoint(),
                        null,
                        message,
                        latest.requestedCancel(),
                        latest.paused(),
                        latest.createdAt(),
                        Instant.now(),
                        Instant.now()));
    }

    private <T> T runInTransaction(java.util.function.Supplier<T> action) {
        return transactions == null ? action.get() : transactions.execute(status -> action.get());
    }

    private void runInTransaction(Runnable action) {
        if (transactions == null) {
            action.run();
        } else {
            transactions.executeWithoutResult(status -> action.run());
        }
    }

    public Optional<SpecBreakdownProcessingJob> getJob(AuthenticatedUser authenticatedUser, UUID jobId) {
        return jobs.findById(jobId).filter(job -> getDraft(authenticatedUser, job.draftId()).isPresent());
    }

    @Transactional
    public SpecBreakdownProcessingJob commandJob(AuthenticatedUser authenticatedUser, UUID jobId, String command) {
        SpecBreakdownProcessingJob job = getJob(authenticatedUser, jobId).orElseThrow();
        SpecBreakdownJobStatus nextStatus = statusForCommand(job.status(), command);
        return jobs.save(
                new SpecBreakdownProcessingJob(
                        job.id(),
                        job.version(),
                        job.draftId(),
                        job.userId(),
                        job.aiJobType(),
                        nextStatus,
                        job.checkpoint(),
                        job.novaRunId(),
                        job.errorMessage(),
                        "cancel".equals(command),
                        "pause".equals(command),
                        job.createdAt(),
                        Instant.now(),
                        nextStatus == SpecBreakdownJobStatus.CANCELED ? Instant.now() : job.completedAt()));
    }

    private SpecBreakdownJobStatus statusForCommand(
            SpecBreakdownJobStatus currentStatus, String command) {
        return switch (command) {
            case "pause" -> {
                if (currentStatus != SpecBreakdownJobStatus.QUEUED
                        && currentStatus != SpecBreakdownJobStatus.RUNNING) {
                    throw new IllegalArgumentException(
                            "Spec breakdown job can only be paused from QUEUED or RUNNING");
                }
                yield SpecBreakdownJobStatus.PAUSED;
            }
            case "resume" -> {
                if (currentStatus != SpecBreakdownJobStatus.PAUSED) {
                    throw new IllegalArgumentException(
                            "Spec breakdown job can only be resumed from PAUSED");
                }
                yield SpecBreakdownJobStatus.QUEUED;
            }
            case "cancel" -> {
                if (currentStatus == SpecBreakdownJobStatus.SUCCEEDED
                        || currentStatus == SpecBreakdownJobStatus.FAILED
                        || currentStatus == SpecBreakdownJobStatus.CANCELED) {
                    throw new IllegalArgumentException(
                            "Spec breakdown job can only be canceled while non-terminal");
                }
                yield SpecBreakdownJobStatus.CANCELED;
            }
            default -> throw new IllegalArgumentException("Unsupported command");
        };
    }

    @Transactional
    public SpecBreakdownDraft review(AuthenticatedUser authenticatedUser, UUID draftId, ReviewCommand command) {
        SpecBreakdownDraft draft = require(authenticatedUser, draftId);
        return drafts.save(
                new SpecBreakdownDraft(
                        draft.id(),
                        draft.version(),
                        draft.projectId(),
                        draft.ownerUserId(),
                        draft.templateId(),
                        draft.title(),
                        draft.rawSpec(),
                        draft.richContent(),
                        command.candidateTree() != null ? command.candidateTree() : draft.candidateTree(),
                        command.accepted()
                                ? SpecBreakdownStatus.READY_FOR_REVIEW
                                : SpecBreakdownStatus.REJECTED,
                        draft.fixVersion(),
                        draft.affectedVersion(),
                        draft.sprint(),
                        draft.issueType(),
                        draft.publishKey(),
                        draft.materializedAt(),
                        draft.createdAt(),
                        Instant.now()));
    }

    @Transactional
    public List<UUID> materialize(AuthenticatedUser authenticatedUser, UUID draftId) {
        SpecBreakdownDraft draft = require(authenticatedUser, draftId);
        if (draft.materializedAt() != null || draft.status() == SpecBreakdownStatus.MATERIALIZED) {
            return List.of();
        }
        List<UUID> ids = new ArrayList<>();
        try {
            JsonNode nodes = mapper.readTree(draft.candidateTree()).path("nodes");
            for (JsonNode node : nodes) {
                ids.add(createTask(authenticatedUser, draft, node, null));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid candidate tree", e);
        }
        drafts.save(
                new SpecBreakdownDraft(
                        draft.id(),
                        draft.version(),
                        draft.projectId(),
                        draft.ownerUserId(),
                        draft.templateId(),
                        draft.title(),
                        draft.rawSpec(),
                        draft.richContent(),
                        draft.candidateTree(),
                        SpecBreakdownStatus.MATERIALIZED,
                        draft.fixVersion(),
                        draft.affectedVersion(),
                        draft.sprint(),
                        draft.issueType(),
                        draft.publishKey(),
                        Instant.now(),
                        draft.createdAt(),
                        Instant.now()));
        return ids;
    }

    private UUID createTask(AuthenticatedUser authenticatedUser, SpecBreakdownDraft draft, JsonNode node, UUID parent) {
        TaskLevel level = deriveTaskLevel(node, parent);
        String type = deriveTaskType(level);
        Integer storyPoints = parseStoryPoints(node);
        CreateTaskCommand command =
                buildGeneratedTaskCommand(draft, node, parent, level, type, storyPoints);

        Task task = tasks.create(authenticatedUser, command);
        UUID taskId = task.id();
        createChildTasks(authenticatedUser, draft, node, taskId);
        return taskId;
    }

    private TaskLevel deriveTaskLevel(JsonNode node, UUID parent) {
        String defaultLevel = parent == null ? "EPIC" : "SUBTASK";
        return TaskLevel.valueOf(node.path("level").asText(defaultLevel));
    }

    private String deriveTaskType(TaskLevel level) {
        if (level == TaskLevel.EPIC) {
            return "EPIC";
        }
        if (level == TaskLevel.STORY) {
            return "STORY";
        }
        return "SUBTASK";
    }

    private Integer parseStoryPoints(JsonNode node) {
        JsonNode storyPoints = node.path("storyPoints");
        if (storyPoints.isMissingNode()) {
            return null;
        }
        return storyPoints.asInt();
    }

    private CreateTaskCommand buildGeneratedTaskCommand(
            SpecBreakdownDraft draft,
            JsonNode node,
            UUID parent,
            TaskLevel level,
            String type,
            Integer storyPoints) {
        return new CreateTaskCommand(
                draft.ownerUserId(),
                draft.projectId(),
                null,
                parent,
                level,
                type,
                storyPoints,
                draft.fixVersion(),
                node.path("title").asText("Generated task"),
                node.path("description").asText(null),
                TaskStatus.TODO,
                GENERATED_TASK_PRIORITY,
                null,
                null,
                null,
                TaskSource.AI_CAPTURE,
                GENERATED_TASK_CONFIDENCE);
    }

    private void createChildTasks(
            AuthenticatedUser authenticatedUser, SpecBreakdownDraft draft, JsonNode node, UUID parent) {
        for (JsonNode child : node.path("children")) {
            createTask(authenticatedUser, draft, child, parent);
        }
    }

    private SpecBreakdownDraft require(AuthenticatedUser authenticatedUser, UUID draftId) {
        return getDraft(authenticatedUser, draftId)
                .orElseThrow(() -> new IllegalArgumentException("Spec breakdown draft not found"));
    }

    private String capability(SpecBreakdownJobType type) {
        return switch (type) {
            case OUTLINE -> "spec-outline";
            case ENRICH -> "spec-enrich";
            case BREAKDOWN -> "spec-breakdown";
            case SECTION -> "spec-breakdown-section";
            case MERGE -> "spec-merge";
        };
    }

    public record CreateDraftCommand(
            UUID projectId,
            UUID templateId,
            String title,
            String rawSpec,
            String richContent,
            String candidateTree,
            String fixVersion,
            String affectedVersion,
            String sprint,
            String issueType,
            String publishKey) {}

    public record ReviewCommand(boolean accepted, String candidateTree) {}
}
