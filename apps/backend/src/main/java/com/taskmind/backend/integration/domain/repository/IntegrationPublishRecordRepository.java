package com.taskmind.backend.integration.domain.repository;

import com.taskmind.backend.integration.domain.model.*;
import java.util.*;
public interface IntegrationPublishRecordRepository {
    IntegrationPublishRecord save(IntegrationPublishRecord record);
    Optional<IntegrationPublishRecord> findByTaskIdAndProjectLinkId(UUID taskId, UUID projectLinkId);
}
