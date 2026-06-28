package com.taskmind.backend.aitaskresolution.domain;

import java.util.*;

public interface AiTaskResolutionJobRepository {
    AiTaskResolutionJob save(AiTaskResolutionJob job);
    Optional<AiTaskResolutionJob> findById(UUID id);
    Optional<AiTaskResolutionJob> findByTaskAndRequesterAndIdempotencyKey(UUID taskId, UUID requestedBy, String key);
    List<AiTaskResolutionJob> findByTaskId(UUID taskId);
    Optional<AiTaskResolutionJob> findFirstQueued();
}
