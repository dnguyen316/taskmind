package com.taskmind.backend.aitaskresolution.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiTaskResolutionActionProposalRepository {
    AiTaskResolutionActionProposal save(AiTaskResolutionActionProposal proposal);

    Optional<AiTaskResolutionActionProposal> findById(UUID id);

    List<AiTaskResolutionActionProposal> findByJobId(UUID jobId);
}
