package com.taskmind.backend.project.domain.repository;

import com.taskmind.backend.project.domain.model.ProjectMembership;
import java.util.List;
import java.util.UUID;

public interface ProjectMembershipRepository {

    ProjectMembership save(ProjectMembership membership);

    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMembership> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);
}
