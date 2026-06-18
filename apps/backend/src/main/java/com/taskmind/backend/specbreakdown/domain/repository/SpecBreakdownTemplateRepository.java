package com.taskmind.backend.specbreakdown.domain.repository;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecBreakdownTemplateRepository {
    SpecBreakdownTemplate save(SpecBreakdownTemplate template);

    Optional<SpecBreakdownTemplate> findById(UUID id);

    List<SpecBreakdownTemplate> findByProjectId(UUID projectId);

    void delete(SpecBreakdownTemplate template);
}
