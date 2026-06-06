package com.taskmind.backend.scheduler.infrastructure.persistence.jpa;

import com.taskmind.backend.scheduler.domain.model.ScheduledBlock;
import com.taskmind.backend.scheduler.domain.repository.ScheduledBlockRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaScheduledBlockRepository implements ScheduledBlockRepository {
    private final SpringDataScheduledBlockJpaRepository repo;

    public JpaScheduledBlockRepository(SpringDataScheduledBlockJpaRepository repo) {
        this.repo = repo;
    }

    @Override
    public ScheduledBlock save(ScheduledBlock block) {
        return repo.saveAndFlush(ScheduledBlockJpaEntity.fromDomain(block)).toDomain();
    }

    @Override
    public List<ScheduledBlock> saveAll(List<ScheduledBlock> blocks) {
        return repo
                .saveAllAndFlush(blocks.stream().map(ScheduledBlockJpaEntity::fromDomain).toList())
                .stream()
                .map(ScheduledBlockJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<ScheduledBlock> findById(UUID id) {
        return repo.findActiveById(id).map(ScheduledBlockJpaEntity::toDomain);
    }

    @Override
    public Optional<ScheduledBlock> findByIdForUpdate(UUID id) {
        return repo.findByIdForUpdate(id).map(ScheduledBlockJpaEntity::toDomain);
    }

    @Override
    public List<ScheduledBlock> findByUserIdBetween(
            UUID userId, OffsetDateTime from, OffsetDateTime to) {
        return repo.findByUserIdBetween(userId, from, to).stream()
                .map(ScheduledBlockJpaEntity::toDomain)
                .toList();
    }
}
