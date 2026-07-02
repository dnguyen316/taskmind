package com.taskmind.backend.aiproposal.domain;

import com.taskmind.ai.contracts.proposal.AiActionProposalContract;
import com.taskmind.ai.contracts.proposal.AiActionProposalStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiActionProposalRepository {
    AiActionProposalContract save(AiActionProposalContract proposal);

    Optional<AiActionProposalContract> findById(UUID id);

    List<AiActionProposalContract> findByUserIdAndStatus(
            UUID userId, AiActionProposalStatus status);
}
