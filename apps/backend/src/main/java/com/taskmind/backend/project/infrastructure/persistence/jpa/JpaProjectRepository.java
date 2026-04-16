package com.taskmind.backend.project.infrastructure.persistence.jpa;

import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectRepository implements ProjectRepository {

    private final SpringDataProjectJpaRepository projectJpaRepository;

    public JpaProjectRepository(SpringDataProjectJpaRepository projectJpaRepository) {
        this.projectJpaRepository = projectJpaRepository;
    }

    @Override
    public Project save(Project project) {
        var persisted = projectJpaRepository.save(ProjectJpaEntity.fromDomain(project));
        return persisted.toDomain();
    }

    @Override
    public Optional<Project> findById(UUID id) {
        return projectJpaRepository.findById(id).map(ProjectJpaEntity::toDomain);
    }

    @Override
    public List<Project> findAll() {
        return projectJpaRepository.findAll().stream().map(ProjectJpaEntity::toDomain).toList();
    }

    @Override
    public boolean existsByKey(String key) {
        return projectJpaRepository.existsByKeyIgnoreCase(key);
    }
}
