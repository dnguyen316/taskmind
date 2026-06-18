package com.taskmind.backend.specbreakdown.domain.repository;

import com.taskmind.backend.specbreakdown.application.SpecBreakdownProcessingJob;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobStatus;
import java.util.*;

public interface SpecBreakdownJobRepository {
    SpecBreakdownProcessingJob save(SpecBreakdownProcessingJob job);

    Optional<SpecBreakdownProcessingJob> findById(UUID id);

    List<SpecBreakdownProcessingJob> findByDraftId(UUID draftId);

    Optional<SpecBreakdownProcessingJob> findFirstByStatusOrderByCreatedAt(SpecBreakdownJobStatus status);
}
