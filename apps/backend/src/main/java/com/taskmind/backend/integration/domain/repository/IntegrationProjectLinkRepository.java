package com.taskmind.backend.integration.domain.repository;

import com.taskmind.backend.integration.domain.model.IntegrationProjectLink;
import java.util.*;

public interface IntegrationProjectLinkRepository {
    IntegrationProjectLink save(IntegrationProjectLink link);
    List<IntegrationProjectLink> findByProjectId(UUID projectId);
    Optional<IntegrationProjectLink> findById(UUID id);
}
