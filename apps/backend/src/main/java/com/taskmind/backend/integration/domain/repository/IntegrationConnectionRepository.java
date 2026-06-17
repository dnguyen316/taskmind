package com.taskmind.backend.integration.domain.repository;

import com.taskmind.backend.integration.domain.model.IntegrationConnection;
import java.util.*;

public interface IntegrationConnectionRepository {
    IntegrationConnection save(IntegrationConnection connection);
    List<IntegrationConnection> findByOwnerUserId(UUID ownerUserId);
    Optional<IntegrationConnection> findById(UUID id);
}
