package com.taskmind.events;

import java.util.Set;

public final class EventTypeRegistry {
    private static final Set<String> TYPES =
            Set.of(
                    EventTypes.TASK_CREATED,
                    EventTypes.TASK_UPDATED,
                    EventTypes.TASK_STATUS_CHANGED,
                    EventTypes.TASK_COMPLETED,
                    EventTypes.TASK_ARCHIVED,
                    EventTypes.TASK_DELETED,
                    EventTypes.PROJECT_CREATED,
                    EventTypes.PROJECT_UPDATED,
                    EventTypes.PROJECT_ARCHIVED,
                    EventTypes.AI_CAPTURE_SUBMITTED,
                    EventTypes.AI_SUGGESTION_ACCEPTED,
                    EventTypes.AI_SUGGESTION_REJECTED,
                    EventTypes.AI_SPEC_BREAKDOWN_COMPLETED,
                    EventTypes.AI_SPEC_BREAKDOWN_FAILED,
                    EventTypes.PLANNER_DAILY_GENERATED,
                    EventTypes.PLANNER_OVERFLOW,
                    EventTypes.PLANNER_CONFIRMED,
                    EventTypes.SCHEDULER_BLOCK_COMPLETED,
                    EventTypes.SCHEDULER_BLOCK_MISSED,
                    EventTypes.REVIEW_WEEKLY_GENERATED,
                    EventTypes.REVIEW_RECOMMENDATION_ADOPTED);

    private EventTypeRegistry() {}

    public static boolean isKnown(String eventType) {
        return TYPES.contains(eventType);
    }

    public static Set<String> knownTypes() {
        return TYPES;
    }
}
