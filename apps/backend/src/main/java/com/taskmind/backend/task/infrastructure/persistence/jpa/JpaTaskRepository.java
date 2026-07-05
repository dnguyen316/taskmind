package com.taskmind.backend.task.infrastructure.persistence.jpa;

import com.taskmind.backend.task.application.TaskQuery;
import com.taskmind.backend.task.domain.model.*;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class JpaTaskRepository implements TaskRepository {
    private final SpringDataTaskJpaRepository repo;

    public JpaTaskRepository(SpringDataTaskJpaRepository repo) {
        this.repo = repo;
    }

    public Task save(Task t) {
        return repo.saveAndFlush(TaskJpaEntity.fromDomain(t)).toDomain();
    }

    public Optional<Task> findById(UUID id) {
        return repo.findActiveById(id).map(TaskJpaEntity::toDomain);
    }

    public Optional<Task> findByIdForUpdate(UUID id) {
        return repo.findByIdForUpdate(id).map(TaskJpaEntity::toDomain);
    }

    public List<Task> findAll() {
        return repo.findAll().stream()
                .map(TaskJpaEntity::toDomain)
                .filter(t -> t.deletedAt() == null)
                .toList();
    }

    public List<Task> findChildren(UUID id) {
        return repo.findChildren(id).stream().map(TaskJpaEntity::toDomain).toList();
    }

    public List<Task> findAncestors(UUID id) {
        ArrayList<Task> out = new ArrayList<Task>();
        Optional<Task> current = findById(id);
        while (current.isPresent() && current.get().parentTaskId() != null) {
            current = findById(current.get().parentTaskId());
            current.ifPresent(out::add);
            if (out.size() > 4) break;
        }
        return out;
    }

    public List<TaskReleaseStatsProjection> releaseStats(UUID id) {
        return repo.releaseStats(id);
    }

    public List<Task> findFiltered(
            Optional<UUID> u, Optional<TaskStatus> s, boolean o, OffsetDateTime n, int p, int z) {
        return repo
                .findFiltered(
                        u.orElse(null),
                        s.orElse(null),
                        o,
                        n,
                        TaskStatus.DONE,
                        TaskStatus.ARCHIVED,
                        PageRequest.of(p, z))
                .stream()
                .map(TaskJpaEntity::toDomain)
                .toList();
    }

    public List<Task> findFiltered(TaskQuery q, OffsetDateTime now, OffsetDateTime todayStart, OffsetDateTime tomorrowStart, java.time.Instant staleBefore) {
        return repo
                .findRichFiltered(
                        q.userId(),
                        q.status(),
                        q.priority(),
                        q.projectId(),
                        q.assigneeId(),
                        Boolean.TRUE.equals(q.dueToday()),
                        Boolean.TRUE.equals(q.overdue()),
                        Boolean.TRUE.equals(q.blocked()),
                        Boolean.TRUE.equals(q.unassigned()),
                        Boolean.TRUE.equals(q.noDueDate()),
                        Boolean.TRUE.equals(q.stale()),
                        Boolean.TRUE.equals(q.archived()),
                        now,
                        todayStart,
                        tomorrowStart,
                        staleBefore,
                        TaskStatus.DONE,
                        TaskStatus.ARCHIVED,
                        q.sort() == null ? "updatedAt" : q.sort(),
                        PageRequest.of(q.page(), q.size()))
                .stream()
                .map(TaskJpaEntity::toDomain)
                .toList();
    }

}
