package com.taskmind.backend.aiproposal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.proposal.*;
import com.taskmind.backend.aiproposal.domain.AiActionProposalRepository;
import com.taskmind.backend.auth.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import org.junit.jupiter.api.Test;

class AiActionProposalApplicationServiceTest {
    private final InMemoryRepository repository = new InMemoryRepository();
    private final AiActionProposalApplicationService service =
            new AiActionProposalApplicationService(
                    repository, Clock.fixed(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC));
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();
    private final AuthenticatedUser user = new AuthenticatedUser(userId, Set.of("USER"));

    @Test
    void aiTaskMutationProposalStaysPendingUntilExplicitCoreApproval() {
        AiActionProposalContract created =
                service.create(proposal(AiActionProposalType.UPDATE_TASK));

        assertThat(service.listPending(user))
                .extracting(AiActionProposalContract::id)
                .containsExactly(created.id());
        assertThat(repository.findById(created.id()).orElseThrow().status())
                .isEqualTo(AiActionProposalStatus.PENDING);
        assertThat(repository.findById(created.id()).orElseThrow().acceptedAt()).isNull();
    }

    @Test
    void acceptsOnlyPendingOwnProposalThroughDecisionPath() {
        AiActionProposalContract created =
                service.create(proposal(AiActionProposalType.CREATE_TASK));

        AiActionProposalContract accepted = service.accept(user, created.id()).orElseThrow();

        assertThat(accepted.status()).isEqualTo(AiActionProposalStatus.ACCEPTED);
        assertThat(accepted.decidedBy()).isEqualTo(userId);
        assertThat(accepted.acceptedAt()).isEqualTo(Instant.parse("2026-07-02T00:00:00Z"));
        assertThat(service.accept(user, created.id())).isEmpty();
    }

    private AiActionProposalContract proposal(AiActionProposalType type) {
        return new AiActionProposalContract(
                null,
                userId,
                type,
                AiActionProposalStatus.ACCEPTED,
                objectMapper.createObjectNode().put("taskId", UUID.randomUUID().toString()),
                "Update task",
                "AI suggested a task change",
                "nova",
                "mock",
                "mock-model",
                AiActionProposalSource.NOVA_CHAT,
                "chat",
                null,
                null,
                null,
                null,
                null);
    }

    private static class InMemoryRepository implements AiActionProposalRepository {
        private final Map<UUID, AiActionProposalContract> proposals = new LinkedHashMap<>();

        @Override
        public AiActionProposalContract save(AiActionProposalContract proposal) {
            proposals.put(proposal.id(), proposal);
            return proposal;
        }

        @Override
        public Optional<AiActionProposalContract> findById(UUID id) {
            return Optional.ofNullable(proposals.get(id));
        }

        @Override
        public List<AiActionProposalContract> findByUserIdAndStatus(
                UUID userId, AiActionProposalStatus status) {
            return proposals.values().stream()
                    .filter(
                            proposal ->
                                    proposal.userId().equals(userId) && proposal.status() == status)
                    .toList();
        }
    }
}
