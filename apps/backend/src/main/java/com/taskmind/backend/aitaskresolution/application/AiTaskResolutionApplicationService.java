package com.taskmind.backend.aitaskresolution.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.contracts.capability.CapabilityResponse;
import com.taskmind.backend.ai.NovaClient;
import com.taskmind.backend.aitaskresolution.domain.*;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional(readOnly = true)
public class AiTaskResolutionApplicationService {
    private static final String CAPABILITY_ID = "task-resolution-agent";
    private final AiTaskResolutionJobRepository jobs;
    private final TaskRepository tasks;
    private final NovaClient nova;
    private final AiTaskResolutionActionProposalRepository proposals;
    private final ObjectMapper mapper;
    private final AiTaskResolutionEventPublisher events;
    private final TransactionTemplate tx;

    public AiTaskResolutionApplicationService(
            AiTaskResolutionJobRepository jobs,
            TaskRepository tasks,
            NovaClient nova,
            AiTaskResolutionActionProposalRepository proposals,
            ObjectMapper mapper,
            AiTaskResolutionEventPublisher events,
            PlatformTransactionManager tm) {
        this.jobs = jobs;
        this.tasks = tasks;
        this.nova = nova;
        this.proposals = proposals;
        this.mapper = mapper;
        this.events = events;
        this.tx = new TransactionTemplate(tm);
    }

    @Transactional
    public AiTaskResolutionJob create(
            AuthenticatedUser user, UUID taskId, CreateAiTaskResolutionJobCommand command) {
        Task task =
                tasks.findById(taskId)
                        .filter(taskCandidate -> canRead(user, taskCandidate))
                        .orElseThrow(() -> new NoSuchElementException("Task not found"));
        String key = command.idempotencyKey();
        if (key != null && !key.isBlank()) {
            Optional<AiTaskResolutionJob> existing =
                    jobs.findByTaskAndRequesterAndIdempotencyKey(taskId, user.userId(), key.trim());
            if (existing.isPresent()) return existing.get();
        }
        Instant now = Instant.now();
        AiTaskResolutionJob job =
                new AiTaskResolutionJob(
                        UUID.randomUUID(),
                        task.id(),
                        task.projectId(),
                        command.templateId(),
                        command.githubProjectLinkId(),
                        AiTaskResolutionJobStatus.QUEUED,
                        user.userId(),
                        key == null ? null : key.trim(),
                        null,
                        "queued",
                        null,
                        null,
                        now,
                        now,
                        null);
        AiTaskResolutionJob saved = jobs.save(job);
        events.statusChanged(saved);
        return saved;
    }

    public List<AiTaskResolutionJob> list(AuthenticatedUser user, UUID taskId) {
        Task task =
                tasks.findById(taskId)
                        .filter(taskCandidate -> canRead(user, taskCandidate))
                        .orElseThrow(() -> new NoSuchElementException("Task not found"));
        return jobs.findByTaskId(task.id());
    }

    public Optional<AiTaskResolutionJob> get(AuthenticatedUser user, UUID id) {
        return jobs.findById(id)
                .filter(job -> tasks.findById(job.taskId()).filter(taskCandidate -> canRead(user, taskCandidate)).isPresent());
    }

    public List<AiTaskResolutionActionProposal> listProposals(AuthenticatedUser user, UUID jobId) {
        AiTaskResolutionJob job =
                get(user, jobId).orElseThrow(() -> new NoSuchElementException("Job not found"));
        return proposals.findByJobId(job.id());
    }

    @Transactional
    public Optional<AiTaskResolutionActionProposal> approveProposal(
            AuthenticatedUser user, UUID jobId, UUID proposalId) {
        return decideProposal(
                user, jobId, proposalId, AiTaskResolutionActionProposalStatus.APPROVED);
    }

    @Transactional
    public Optional<AiTaskResolutionActionProposal> rejectProposal(
            AuthenticatedUser user, UUID jobId, UUID proposalId) {
        Optional<AiTaskResolutionActionProposal> decided =
                decideProposal(
                        user, jobId, proposalId, AiTaskResolutionActionProposalStatus.REJECTED);
        decided.ifPresent(
                proposal ->
                        transition(
                                user,
                                jobId,
                                AiTaskResolutionJobStatus.PAUSED,
                                "proposal_rejected",
                                "ACTION_REJECTED"));
        return decided;
    }

    private Optional<AiTaskResolutionActionProposal> decideProposal(
            AuthenticatedUser user,
            UUID jobId,
            UUID proposalId,
            AiTaskResolutionActionProposalStatus status) {
        AiTaskResolutionJob job =
                get(user, jobId).orElseThrow(() -> new NoSuchElementException("Job not found"));
        if (!user.isPrivileged() && !user.userId().equals(job.requestedBy()))
            throw new AiTaskResolutionForbiddenException(
                    "Not allowed to approve AI action proposals");
        Instant now = Instant.now();
        return proposals
                .findById(proposalId)
                .filter(proposal -> proposal.jobId().equals(job.id()))
                .filter(proposal -> proposal.status() == AiTaskResolutionActionProposalStatus.PENDING)
                .map(
                        proposal ->
                                proposals.save(
                                        new AiTaskResolutionActionProposal(
                                                proposal.id(),
                                                proposal.jobId(),
                                                proposal.proposedActionType(),
                                                proposal.payloadPreview(),
                                                proposal.riskLevel(),
                                                proposal.rationale(),
                                                status,
                                                user.userId(),
                                                now,
                                                null,
                                                proposal.createdAt(),
                                                now)));
    }

    @Transactional
    public Optional<AiTaskResolutionJob> cancel(AuthenticatedUser user, UUID id) {
        return transition(user, id, AiTaskResolutionJobStatus.CANCELED, "canceled", null);
    }

    @Transactional
    public Optional<AiTaskResolutionJob> approve(AuthenticatedUser user, UUID id) {
        return transition(user, id, AiTaskResolutionJobStatus.QUEUED, "approved", null);
    }

    private Optional<AiTaskResolutionJob> transition(
            AuthenticatedUser authenticatedUser,
            UUID id,
            AiTaskResolutionJobStatus status,
            String step,
            String error) {
        return get(authenticatedUser, id)
                .map(
                        job ->
                                saveWithEvent(
                                        new AiTaskResolutionJob(
                                                job.id(),
                                                job.taskId(),
                                                job.projectId(),
                                                job.templateId(),
                                                job.githubProjectLinkId(),
                                                status,
                                                job.requestedBy(),
                                                job.idempotencyKey(),
                                                job.novaRunId(),
                                                step,
                                                job.resultSummary(),
                                                error,
                                                job.createdAt(),
                                                Instant.now(),
                                                terminal(status) ? Instant.now() : null)));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean processOneQueuedJob() {
        AiTaskResolutionJob running =
                tx.execute(
                        transactionStatus ->
                                jobs.findFirstQueued()
                                        .map(
                                                job ->
                                                        saveWithEvent(
                                                                new AiTaskResolutionJob(
                                                                        job.id(),
                                                                        job.taskId(),
                                                                        job.projectId(),
                                                                        job.templateId(),
                                                                        job.githubProjectLinkId(),
                                                                        AiTaskResolutionJobStatus
                                                                                .RUNNING,
                                                                        job.requestedBy(),
                                                                        job.idempotencyKey(),
                                                                        job.novaRunId(),
                                                                        "calling_nova",
                                                                        job.resultSummary(),
                                                                        null,
                                                                        job.createdAt(),
                                                                        Instant.now(),
                                                                        null)))
                                        .orElse(null));
        if (running == null) return false;
        try {
            ObjectNode input = mapper.createObjectNode();
            input.putObject("task")
                    .put("id", running.taskId().toString())
                    .put("title", "Task " + running.taskId())
                    .put("description", "Resolve task with AI approval gates")
                    .put("status", "OPEN");
            input.put("projectId", running.projectId().toString());
            input.putObject("githubRepository")
                    .put("owner", "taskmind")
                    .put("name", "taskmind")
                    .put("defaultBranch", "main");
            input.putObject("workflowTemplate")
                    .put("id", "task-resolution-default")
                    .put("version", "1");
            input.putArray("allowedTools").add("core.task.comment");
            input.put("approvalPolicy", "require-approval");
            if (running.templateId() != null)
                input.put("templateId", running.templateId().toString());
            if (running.githubProjectLinkId() != null)
                input.put("githubProjectLinkId", running.githubProjectLinkId().toString());
            CapabilityResponse response =
                    nova.executeCapability(
                            CAPABILITY_ID,
                            new CapabilityRequest(
                                    new AiCapabilityId(CAPABILITY_ID),
                                    running.requestedBy(),
                                    running.projectId().toString(),
                                    input,
                                    running.id().toString(),
                                    idempotencyKey(running)));
            String summary = response.output() == null ? null : response.output().toString();
            if (persistReturnedProposals(running, response.output())) {
                tx.executeWithoutResult(
                        transactionStatus ->
                                saveWithEvent(
                                        new AiTaskResolutionJob(
                                                running.id(),
                                                running.taskId(),
                                                running.projectId(),
                                                running.templateId(),
                                                running.githubProjectLinkId(),
                                                AiTaskResolutionJobStatus.WAITING_FOR_APPROVAL,
                                                running.requestedBy(),
                                                running.idempotencyKey(),
                                                response.runId(),
                                                "awaiting_action_approval",
                                                summary,
                                                null,
                                                running.createdAt(),
                                                Instant.now(),
                                                null)));
            } else {
                tx.executeWithoutResult(
                        transactionStatus ->
                                saveWithEvent(
                                        new AiTaskResolutionJob(
                                                running.id(),
                                                running.taskId(),
                                                running.projectId(),
                                                running.templateId(),
                                                running.githubProjectLinkId(),
                                                AiTaskResolutionJobStatus.SUCCEEDED,
                                                running.requestedBy(),
                                                running.idempotencyKey(),
                                                response.runId(),
                                                "completed",
                                                summary,
                                                null,
                                                running.createdAt(),
                                                Instant.now(),
                                                Instant.now())));
            }
        } catch (Exception ex) {
            tx.executeWithoutResult(
                    transactionStatus ->
                            saveWithEvent(
                                    new AiTaskResolutionJob(
                                            running.id(),
                                            running.taskId(),
                                            running.projectId(),
                                            running.templateId(),
                                            running.githubProjectLinkId(),
                                            AiTaskResolutionJobStatus.FAILED,
                                            running.requestedBy(),
                                            running.idempotencyKey(),
                                            running.novaRunId(),
                                            "failed",
                                            running.resultSummary(),
                                            "NOVA_CAPABILITY_FAILED",
                                            running.createdAt(),
                                            Instant.now(),
                                            Instant.now())));
        }
        return true;
    }

    private boolean persistReturnedProposals(
            AiTaskResolutionJob job, com.fasterxml.jackson.databind.JsonNode output) {
        if (output == null
                || !output.has("proposals")
                || !output.get("proposals").isArray()
                || output.get("proposals").isEmpty()) return false;
        Instant now = Instant.now();
        output.get("proposals")
                .forEach(
                        node ->
                                proposals.save(
                                        new AiTaskResolutionActionProposal(
                                                UUID.randomUUID(),
                                                job.id(),
                                                node.path("proposedActionType")
                                                        .asText(
                                                                node.path("actionType")
                                                                        .asText("UNKNOWN")),
                                                node.path("payloadPreview").isMissingNode()
                                                        ? "{}"
                                                        : node.path("payloadPreview").toString(),
                                                node.path("riskLevel").asText("HIGH"),
                                                node.path("rationale").asText(null),
                                                AiTaskResolutionActionProposalStatus.PENDING,
                                                null,
                                                null,
                                                null,
                                                now,
                                                now)));
        return true;
    }

    private AiTaskResolutionJob saveWithEvent(AiTaskResolutionJob job) {
        AiTaskResolutionJob saved = jobs.save(job);
        events.statusChanged(saved);
        return saved;
    }

    private boolean terminal(AiTaskResolutionJobStatus status) {
        return status == AiTaskResolutionJobStatus.SUCCEEDED
                || status == AiTaskResolutionJobStatus.FAILED
                || status == AiTaskResolutionJobStatus.CANCELED;
    }

    private String idempotencyKey(AiTaskResolutionJob job) {
        return job.idempotencyKey() == null || job.idempotencyKey().isBlank()
                ? job.id().toString()
                : job.idempotencyKey();
    }

    private boolean canRead(AuthenticatedUser authenticatedUser, Task task) {
        return authenticatedUser.isPrivileged()
                || authenticatedUser.userId().equals(task.userId())
                || authenticatedUser.userId().equals(task.assigneeId());
    }
}
