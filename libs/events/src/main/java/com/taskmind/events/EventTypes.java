package com.taskmind.events;

public final class EventTypes {
    public static final String TASK_CREATED = "task.created";
    public static final String TASK_UPDATED = "task.updated";
    public static final String TASK_STATUS_CHANGED = "task.status_changed";
    public static final String TASK_COMPLETED = "task.completed";
    public static final String TASK_ARCHIVED = "task.archived";
    public static final String TASK_DELETED = "task.deleted";
    public static final String PROJECT_CREATED = "project.created";
    public static final String PROJECT_UPDATED = "project.updated";
    public static final String PROJECT_ARCHIVED = "project.archived";
    public static final String AI_CAPTURE_SUBMITTED = "ai.capture_submitted";
    public static final String AI_SUGGESTION_ACCEPTED = "ai.suggestion_accepted";
    public static final String AI_SUGGESTION_REJECTED = "ai.suggestion_rejected";
    public static final String AI_SPEC_BREAKDOWN_COMPLETED = "ai.spec_breakdown_completed";
    public static final String AI_SPEC_BREAKDOWN_FAILED = "ai.spec_breakdown_failed";
    public static final String PLANNER_DAILY_GENERATED = "planner.daily_generated";
    public static final String PLANNER_OVERFLOW = "planner.overflow";
    public static final String PLANNER_CONFIRMED = "planner.confirmed";
    public static final String SCHEDULER_BLOCK_COMPLETED = "scheduler.block_completed";
    public static final String SCHEDULER_BLOCK_MISSED = "scheduler.block_missed";
    public static final String REVIEW_WEEKLY_GENERATED = "review.weekly_generated";
    public static final String REVIEW_RECOMMENDATION_ADOPTED = "review.recommendation_adopted";

    private EventTypes() {}
}
