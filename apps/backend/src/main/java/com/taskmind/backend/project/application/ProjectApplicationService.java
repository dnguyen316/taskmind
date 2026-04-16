package com.taskmind.backend.project.application;

import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProjectApplicationService {

    private final ProjectRepository projectRepository;

    public ProjectApplicationService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public Project create(CreateProjectCommand command) {
        var normalizedKey = command.key().trim().toUpperCase();
        if (projectRepository.existsByKey(normalizedKey)) {
            throw new IllegalArgumentException("Project key already exists");
        }

        var now = Instant.now();
        var project = new Project(
            UUID.randomUUID(),
            null,
            command.name().trim(),
            normalizedKey,
            command.description(),
            command.ownerUserId(),
            null,
            now,
            now
        );
        try {
            return projectRepository.save(project);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Project key already exists", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Project> findById(UUID id) {
        return projectRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Project> list(boolean includeArchived) {
        return projectRepository.findAll().stream()
            .filter(project -> includeArchived || project.archivedAt() == null)
            .sorted(Comparator.comparing(Project::createdAt).reversed())
            .toList();
    }

    @Transactional
    public Optional<Project> update(UUID id, UpdateProjectCommand command) {
        return projectRepository.findById(id)
            .map(existing -> {
                var updatedKey = command.key() != null ? command.key().trim().toUpperCase() : existing.key();
                if (!existing.key().equals(updatedKey) && projectRepository.existsByKey(updatedKey)) {
                    throw new IllegalArgumentException("Project key already exists");
                }

                var updated = new Project(
                    existing.id(),
                    existing.version(),
                    command.name() != null ? command.name().trim() : existing.name(),
                    updatedKey,
                    command.description() != null ? command.description() : existing.description(),
                    existing.ownerUserId(),
                    existing.archivedAt(),
                    existing.createdAt(),
                    Instant.now()
                );
                try {
                    return projectRepository.save(updated);
                } catch (DataIntegrityViolationException e) {
                    throw new IllegalArgumentException("Project key already exists", e);
                }
            });
    }

    @Transactional
    public Optional<Project> archive(ArchiveProjectCommand command) {
        return projectRepository.findById(command.projectId())
            .map(existing -> {
                if (existing.archivedAt() != null) {
                    return existing;
                }
                return projectRepository.save(existing.withArchivedAt(Instant.now(), Instant.now()));
            });
    }
}
