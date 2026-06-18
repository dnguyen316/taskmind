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
import com.taskmind.backend.task.domain.model.TaskType;
import com.taskmind.events.EventTypes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
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
        this.transactions =
                transactionManager == null ? null : new TransactionTemplate(transactionManager);
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
    public SpecBreakdownDraft createDraft(AuthenticatedUser u, CreateDraftCommand c) {
        Instant now = Instant.now();
        String tree = c.candidateTree() != null ? c.candidateTree() : "{\"nodes\":[]}";
        return drafts.save(
                new SpecBreakdownDraft(
                        UUID.randomUUID(),
                        null,
                        c.projectId(),
                        u.userId(),
                        c.templateId(),
                        c.title().trim(),
                        c.rawSpec(),
                        c.richContent(),
                        tree,
                        SpecBreakdownStatus.DRAFT,
                        c.fixVersion(),
                        c.affectedVersion(),
                        c.sprint(),
                        c.issueType(),
                        c.publishKey(),
                        null,
                        now,
                        now));
    }

    public Optional<SpecBreakdownDraft> getDraft(AuthenticatedUser u, UUID id) {
        return drafts.findById(id)
                .filter(d -> u.isPrivileged() || d.ownerUserId().equals(u.userId()));
    }

    @Transactional
    public SpecBreakdownProcessingJob startJob(
            AuthenticatedUser u, UUID draftId, SpecBreakdownJobType type) {
        SpecBreakdownDraft d = require(u, draftId);
        Instant now = Instant.now();
        return jobs.save(
                new SpecBreakdownProcessingJob(
                        UUID.randomUUID(),
                        null,
                        d.id(),
                        u.userId(),
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

    public boolean processOneQueuedJob() {
        SpecBreakdownProcessingJob running = runInTransaction(this::claimNextQueuedJob);
        if (running == null) {
            return false;
        }

        SpecBreakdownDraft d = drafts.findById(running.draftId()).orElseThrow();
        try {
            ObjectNode input = mapper.createObjectNode();
            input.put("draftId", d.id().toString());
            input.put("rawSpec", d.rawSpec());
            input.set("candidateTree", mapper.readTree(d.candidateTree()));
            CapabilityResponse response =
                    nova.executeCapability(
                            capability(running.aiJobType()),
                            new CapabilityRequest(
                                    new AiCapabilityId(capability(running.aiJobType())),
                                    running.userId(),
                                    d.projectId().toString(),
                                    input,
                                    running.id().toString(),
                                    running.id().toString()));
            String output =
                    response.output() == null
                            ? d.candidateTree()
                            : mapper.writeValueAsString(response.output());
            runInTransaction(() -> completeSucceededJob(d, running, output, response.runId()));
        } catch (Exception e) {
            runInTransaction(() -> completeFailedJob(d, running, e));
        }
        return true;
    }

    private SpecBreakdownProcessingJob claimNextQueuedJob() {
        Optional<SpecBreakdownProcessingJob> next =
                jobs.findFirstByStatusOrderByCreatedAt(SpecBreakdownJobStatus.QUEUED);
        if (next.isEmpty()) {
            return null;
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
            return null;
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
            return null;
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
        return jobs.save(
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
                        null));
    }

    private void completeSucceededJob(
            SpecBreakdownDraft d, SpecBreakdownProcessingJob running, String output, UUID runId) {
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
                        d.id(),
                        d.version(),
                        d.projectId(),
                        d.ownerUserId(),
                        d.templateId(),
                        d.title(),
                        d.rawSpec(),
                        d.richContent(),
                        output,
                        SpecBreakdownStatus.READY_FOR_REVIEW,
                        d.fixVersion(),
                        d.affectedVersion(),
                        d.sprint(),
                        d.issueType(),
                        d.publishKey(),
                        d.materializedAt(),
                        d.createdAt(),
                        Instant.now()));
        events.publish(
                running.userId(),
                EventTypes.AI_SPEC_BREAKDOWN_COMPLETED,
                Map.of(
                        "draftId",
                        d.id().toString(),
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
            SpecBreakdownDraft d, SpecBreakdownProcessingJob running, Exception e) {
        String message = e.getMessage() == null ? "failed" : e.getMessage();
        events.publish(
                running.userId(),
                EventTypes.AI_SPEC_BREAKDOWN_FAILED,
                Map.of(
                        "draftId",
                        d.id().toString(),
                        "jobId",
                        running.id().toString(),
                        "message",
                        message));
        drafts.save(
                new SpecBreakdownDraft(
                        d.id(),
                        d.version(),
                        d.projectId(),
                        d.ownerUserId(),
                        d.templateId(),
                        d.title(),
                        d.rawSpec(),
                        d.richContent(),
                        d.candidateTree(),
                        SpecBreakdownStatus.FAILED,
                        d.fixVersion(),
                        d.affectedVersion(),
                        d.sprint(),
                        d.issueType(),
                        d.publishKey(),
                        d.materializedAt(),
                        d.createdAt(),
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

    public Optional<SpecBreakdownProcessingJob> getJob(AuthenticatedUser u, UUID id) {
        return jobs.findById(id).filter(j -> getDraft(u, j.draftId()).isPresent());
    }

    @Transactional
    public SpecBreakdownProcessingJob commandJob(AuthenticatedUser u, UUID id, String command) {
        SpecBreakdownProcessingJob j = getJob(u, id).orElseThrow();
        SpecBreakdownJobStatus s =
                switch (command) {
                    case "pause" -> SpecBreakdownJobStatus.PAUSED;
                    case "resume" -> SpecBreakdownJobStatus.QUEUED;
                    case "cancel" -> SpecBreakdownJobStatus.CANCELED;
                    default -> throw new IllegalArgumentException("Unsupported command");
                };
        return jobs.save(
                new SpecBreakdownProcessingJob(
                        j.id(),
                        j.version(),
                        j.draftId(),
                        j.userId(),
                        j.aiJobType(),
                        s,
                        j.checkpoint(),
                        j.novaRunId(),
                        j.errorMessage(),
                        "cancel".equals(command),
                        "pause".equals(command),
                        j.createdAt(),
                        Instant.now(),
                        s == SpecBreakdownJobStatus.CANCELED ? Instant.now() : j.completedAt()));
    }

    @Transactional
    public SpecBreakdownDraft review(AuthenticatedUser u, UUID id, ReviewCommand c) {
        SpecBreakdownDraft d = require(u, id);
        return drafts.save(
                new SpecBreakdownDraft(
                        d.id(),
                        d.version(),
                        d.projectId(),
                        d.ownerUserId(),
                        d.templateId(),
                        d.title(),
                        d.rawSpec(),
                        d.richContent(),
                        c.candidateTree() != null ? c.candidateTree() : d.candidateTree(),
                        c.accepted()
                                ? SpecBreakdownStatus.READY_FOR_REVIEW
                                : SpecBreakdownStatus.REJECTED,
                        d.fixVersion(),
                        d.affectedVersion(),
                        d.sprint(),
                        d.issueType(),
                        d.publishKey(),
                        d.materializedAt(),
                        d.createdAt(),
                        Instant.now()));
    }

    @Transactional
    public List<UUID> materialize(AuthenticatedUser u, UUID id) {
        SpecBreakdownDraft d = require(u, id);
        List<UUID> ids = new ArrayList<>();
        try {
            JsonNode nodes = mapper.readTree(d.candidateTree()).path("nodes");
            for (JsonNode n : nodes) {
                ids.add(createTask(u, d, n, null));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid candidate tree", e);
        }
        drafts.save(
                new SpecBreakdownDraft(
                        d.id(),
                        d.version(),
                        d.projectId(),
                        d.ownerUserId(),
                        d.templateId(),
                        d.title(),
                        d.rawSpec(),
                        d.richContent(),
                        d.candidateTree(),
                        SpecBreakdownStatus.MATERIALIZED,
                        d.fixVersion(),
                        d.affectedVersion(),
                        d.sprint(),
                        d.issueType(),
                        d.publishKey(),
                        Instant.now(),
                        d.createdAt(),
                        Instant.now()));
        return ids;
    }

    private UUID createTask(AuthenticatedUser u, SpecBreakdownDraft d, JsonNode n, UUID parent) {
        TaskLevel level = deriveTaskLevel(n, parent);
        TaskType type = deriveTaskType(level);
        Integer storyPoints = parseStoryPoints(n);
        CreateTaskCommand command =
                buildGeneratedTaskCommand(d, n, parent, level, type, storyPoints);

        Task task = tasks.create(u, command);
        UUID id = task.id();
        createChildTasks(u, d, n, id);
        return id;
    }

    private TaskLevel deriveTaskLevel(JsonNode node, UUID parent) {
        String defaultLevel = parent == null ? "EPIC" : "SUBTASK";
        return TaskLevel.valueOf(node.path("level").asText(defaultLevel));
    }

    private TaskType deriveTaskType(TaskLevel level) {
        if (level == TaskLevel.EPIC) {
            return TaskType.EPIC;
        }
        if (level == TaskLevel.STORY) {
            return TaskType.STORY;
        }
        return TaskType.SUBTASK;
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
            TaskType type,
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
            AuthenticatedUser u, SpecBreakdownDraft d, JsonNode n, UUID parent) {
        for (JsonNode child : n.path("children")) {
            createTask(u, d, child, parent);
        }
    }

    private SpecBreakdownDraft require(AuthenticatedUser u, UUID id) {
        return getDraft(u, id)
                .orElseThrow(() -> new IllegalArgumentException("Spec breakdown draft not found"));
    }

    private String capability(SpecBreakdownJobType t) {
        return switch (t) {
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
