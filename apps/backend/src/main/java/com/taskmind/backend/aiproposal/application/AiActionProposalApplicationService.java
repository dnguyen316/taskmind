package com.taskmind.backend.aiproposal.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskmind.ai.contracts.proposal.AiActionProposalContract;
import com.taskmind.ai.contracts.proposal.AiActionProposalStatus;
import com.taskmind.backend.aiproposal.domain.AiActionProposalRepository;
import com.taskmind.backend.auth.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiActionProposalApplicationService {
    private final AiActionProposalRepository proposals;
    private final Clock clock;

    @Autowired
    public AiActionProposalApplicationService(AiActionProposalRepository proposals) {
        this(proposals, Clock.systemUTC());
    }

    AiActionProposalApplicationService(AiActionProposalRepository proposals, Clock clock) {
        this.proposals = proposals;
        this.clock = clock;
    }

    public AiActionProposalContract create(AiActionProposalContract proposal) {
        Instant now = Instant.now(clock);
        return proposals.save(
                new AiActionProposalContract(
                        proposal.id() == null ? UUID.randomUUID() : proposal.id(),
                        proposal.userId(),
                        proposal.actionType(),
                        AiActionProposalStatus.PENDING,
                        proposal.proposedPayload(),
                        proposal.preview(),
                        proposal.rationale(),
                        proposal.proposer() == null ? "nova" : proposal.proposer(),
                        proposal.provider(),
                        proposal.model(),
                        proposal.source(),
                        proposal.sourceContext(),
                        proposal.createdAt() == null ? now : proposal.createdAt(),
                        proposal.expiresAt(),
                        null,
                        null,
                        null));
    }

    public List<AiActionProposalContract> listPending(AuthenticatedUser user) {
        return proposals
                .findByUserIdAndStatus(user.userId(), AiActionProposalStatus.PENDING)
                .stream()
                .map(this::expireIfNeeded)
                .filter(proposal -> proposal.status() == AiActionProposalStatus.PENDING)
                .toList();
    }

    public Optional<AiProposalImpactPreview> preview(AuthenticatedUser user, UUID proposalId) {
        return findOwnPending(user, proposalId)
                .map(
                        proposal ->
                                new AiProposalImpactPreview(
                                        proposal.id(),
                                        proposal.actionType(),
                                        "Core will apply this "
                                                + proposal.actionType()
                                                + " proposal only after user approval.",
                                        List.of(proposal.actionType().name()),
                                        proposal.proposedPayload()));
    }

    public Optional<AiActionProposalContract> accept(AuthenticatedUser user, UUID proposalId) {
        return decide(user, proposalId, AiActionProposalStatus.ACCEPTED, "accepted", null);
    }

    public Optional<AiActionProposalContract> reject(
            AuthenticatedUser user, UUID proposalId, String reason) {
        return decide(
                user,
                proposalId,
                AiActionProposalStatus.REJECTED,
                reason == null ? "rejected" : reason,
                null);
    }

    public Optional<AiActionProposalContract> acceptWithEdits(
            AuthenticatedUser user, UUID proposalId, JsonNode editedPayload) {
        return decide(
                user,
                proposalId,
                AiActionProposalStatus.EDITED,
                "accepted_with_edits",
                editedPayload);
    }

    private Optional<AiActionProposalContract> decide(
            AuthenticatedUser user,
            UUID proposalId,
            AiActionProposalStatus status,
            String userDecision,
            JsonNode editedPayload) {
        return findOwnPending(user, proposalId)
                .map(
                        proposal ->
                                proposals.save(
                                        new AiActionProposalContract(
                                                proposal.id(),
                                                proposal.userId(),
                                                proposal.actionType(),
                                                status,
                                                editedPayload == null
                                                        ? proposal.proposedPayload()
                                                        : editedPayload,
                                                proposal.preview(),
                                                proposal.rationale(),
                                                proposal.proposer(),
                                                proposal.provider(),
                                                proposal.model(),
                                                proposal.source(),
                                                proposal.sourceContext(),
                                                proposal.createdAt(),
                                                proposal.expiresAt(),
                                                Instant.now(clock),
                                                user.userId(),
                                                userDecision)));
    }

    private Optional<AiActionProposalContract> findOwnPending(
            AuthenticatedUser user, UUID proposalId) {
        return proposals
                .findById(proposalId)
                .filter(proposal -> proposal.userId().equals(user.userId()))
                .map(this::expireIfNeeded)
                .filter(proposal -> proposal.status() == AiActionProposalStatus.PENDING);
    }

    private AiActionProposalContract expireIfNeeded(AiActionProposalContract proposal) {
        if (proposal.status() != AiActionProposalStatus.PENDING
                || proposal.expiresAt() == null
                || proposal.expiresAt().isAfter(Instant.now(clock))) {
            return proposal;
        }
        return proposals.save(
                new AiActionProposalContract(
                        proposal.id(),
                        proposal.userId(),
                        proposal.actionType(),
                        AiActionProposalStatus.EXPIRED,
                        proposal.proposedPayload(),
                        proposal.preview(),
                        proposal.rationale(),
                        proposal.proposer(),
                        proposal.provider(),
                        proposal.model(),
                        proposal.source(),
                        proposal.sourceContext(),
                        proposal.createdAt(),
                        proposal.expiresAt(),
                        proposal.acceptedAt(),
                        proposal.decidedBy(),
                        "expired"));
    }
}
