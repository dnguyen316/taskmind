package com.taskmind.backend.integration.domain.repository;

import com.taskmind.backend.integration.domain.model.*;
import java.util.*;
public interface IntegrationExternalLinkRepository {
    IntegrationExternalLink save(IntegrationExternalLink link);
    Optional<IntegrationExternalLink> findByTaskIdAndProvider(UUID taskId, IntegrationProvider provider);
    Optional<IntegrationExternalLink> findByProviderAndExternalTypeAndExternalIdentity(IntegrationProvider provider, String externalType, String externalId, String externalKey);
}
