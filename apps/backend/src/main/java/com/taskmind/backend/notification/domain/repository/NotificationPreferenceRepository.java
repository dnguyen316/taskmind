package com.taskmind.backend.notification.domain.repository;

import com.taskmind.backend.notification.domain.model.NotificationPreference;
import java.util.*;

public interface NotificationPreferenceRepository {
    Optional<NotificationPreference> findByUserId(UUID userId);

    NotificationPreference save(NotificationPreference preference);
}
