package com.taskmind.backend.specbreakdown.domain.repository;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownAttachment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpecBreakdownAttachmentRepository {
    SpecBreakdownAttachment save(SpecBreakdownAttachment attachment);

    Optional<SpecBreakdownAttachment> findActiveById(UUID id);

    List<SpecBreakdownAttachment> findActiveByDraftId(UUID draftId);
}
