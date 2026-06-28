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
    private static final String CAPABILITY_ID = "task-resolution";
    private final AiTaskResolutionJobRepository jobs;
    private final TaskRepository tasks;
    private final NovaClient nova;
    private final ObjectMapper mapper;
    private final AiTaskResolutionEventPublisher events;
    private final TransactionTemplate tx;

    public AiTaskResolutionApplicationService(AiTaskResolutionJobRepository jobs, TaskRepository tasks, NovaClient nova, ObjectMapper mapper, AiTaskResolutionEventPublisher events, PlatformTransactionManager tm) {
        this.jobs = jobs; this.tasks = tasks; this.nova = nova; this.mapper = mapper; this.events = events; this.tx = new TransactionTemplate(tm);
    }

    @Transactional
    public AiTaskResolutionJob create(AuthenticatedUser user, UUID taskId, CreateAiTaskResolutionJobCommand c) {
        Task task = tasks.findById(taskId).filter(t -> canRead(user, t)).orElseThrow(() -> new NoSuchElementException("Task not found"));
        String key = c.idempotencyKey();
        if (key != null && !key.isBlank()) {
            Optional<AiTaskResolutionJob> existing = jobs.findByTaskAndRequesterAndIdempotencyKey(taskId, user.userId(), key.trim());
            if (existing.isPresent()) return existing.get();
        }
        Instant now = Instant.now();
        AiTaskResolutionJob job = new AiTaskResolutionJob(UUID.randomUUID(), task.id(), task.projectId(), c.templateId(), c.githubProjectLinkId(), AiTaskResolutionJobStatus.QUEUED, user.userId(), key == null ? null : key.trim(), null, "queued", null, null, now, now, null);
        AiTaskResolutionJob saved = jobs.save(job);
        events.statusChanged(saved);
        return saved;
    }

    public List<AiTaskResolutionJob> list(AuthenticatedUser user, UUID taskId) {
        Task task = tasks.findById(taskId).filter(t -> canRead(user, t)).orElseThrow(() -> new NoSuchElementException("Task not found"));
        return jobs.findByTaskId(task.id());
    }

    public Optional<AiTaskResolutionJob> get(AuthenticatedUser user, UUID id) { return jobs.findById(id).filter(j -> tasks.findById(j.taskId()).filter(t -> canRead(user, t)).isPresent()); }

    @Transactional
    public Optional<AiTaskResolutionJob> cancel(AuthenticatedUser user, UUID id) { return transition(user, id, AiTaskResolutionJobStatus.CANCELED, "canceled", null); }

    @Transactional
    public Optional<AiTaskResolutionJob> approve(AuthenticatedUser user, UUID id) { return transition(user, id, AiTaskResolutionJobStatus.QUEUED, "approved", null); }

    private Optional<AiTaskResolutionJob> transition(AuthenticatedUser u, UUID id, AiTaskResolutionJobStatus status, String step, String error) {
        return get(u, id).map(j -> saveWithEvent(new AiTaskResolutionJob(j.id(), j.taskId(), j.projectId(), j.templateId(), j.githubProjectLinkId(), status, j.requestedBy(), j.idempotencyKey(), j.novaRunId(), step, j.resultSummary(), error, j.createdAt(), Instant.now(), terminal(status) ? Instant.now() : null)));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean processOneQueuedJob() {
        AiTaskResolutionJob running = tx.execute(s -> jobs.findFirstQueued().map(j -> saveWithEvent(new AiTaskResolutionJob(j.id(), j.taskId(), j.projectId(), j.templateId(), j.githubProjectLinkId(), AiTaskResolutionJobStatus.RUNNING, j.requestedBy(), j.idempotencyKey(), j.novaRunId(), "calling_nova", j.resultSummary(), null, j.createdAt(), Instant.now(), null))).orElse(null));
        if (running == null) return false;
        try {
            ObjectNode input = mapper.createObjectNode();
            input.put("taskId", running.taskId().toString());
            if (running.templateId() != null) input.put("templateId", running.templateId().toString());
            if (running.githubProjectLinkId() != null) input.put("githubProjectLinkId", running.githubProjectLinkId().toString());
            CapabilityResponse response = nova.executeCapability(CAPABILITY_ID, new CapabilityRequest(new AiCapabilityId(CAPABILITY_ID), running.requestedBy(), running.projectId().toString(), input, running.id().toString(), idempotencyKey(running)));
            String summary = response.output() == null ? null : response.output().toString();
            tx.executeWithoutResult(s -> saveWithEvent(new AiTaskResolutionJob(running.id(), running.taskId(), running.projectId(), running.templateId(), running.githubProjectLinkId(), AiTaskResolutionJobStatus.SUCCEEDED, running.requestedBy(), running.idempotencyKey(), response.runId(), "completed", summary, null, running.createdAt(), Instant.now(), Instant.now())));
        } catch (Exception ex) {
            tx.executeWithoutResult(s -> saveWithEvent(new AiTaskResolutionJob(running.id(), running.taskId(), running.projectId(), running.templateId(), running.githubProjectLinkId(), AiTaskResolutionJobStatus.FAILED, running.requestedBy(), running.idempotencyKey(), running.novaRunId(), "failed", running.resultSummary(), "NOVA_CAPABILITY_FAILED", running.createdAt(), Instant.now(), Instant.now())));
        }
        return true;
    }

    private AiTaskResolutionJob saveWithEvent(AiTaskResolutionJob job) { AiTaskResolutionJob saved = jobs.save(job); events.statusChanged(saved); return saved; }
    private boolean terminal(AiTaskResolutionJobStatus s) { return s == AiTaskResolutionJobStatus.SUCCEEDED || s == AiTaskResolutionJobStatus.FAILED || s == AiTaskResolutionJobStatus.CANCELED; }
    private String idempotencyKey(AiTaskResolutionJob j) { return j.idempotencyKey() == null || j.idempotencyKey().isBlank() ? j.id().toString() : j.idempotencyKey(); }
    private boolean canRead(AuthenticatedUser u, Task t) { return u.isPrivileged() || u.userId().equals(t.userId()) || u.userId().equals(t.assigneeId()); }
}
