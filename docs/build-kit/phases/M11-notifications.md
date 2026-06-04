# M11 - Notifications

## Objective

Deliver the TaskMind notification milestone in **Core** and the **frontend**: in-app
notifications, a Server-Sent Events (SSE) live stream, per-user notification preferences,
reminder and digest scheduled jobs, and Slack delivery. Notifications are triggered by
Core-domain activity such as task assignment, due-date/reminder events, and new task
comments.

## Depends on

- [M02 - Tasks + Projects](M02-tasks-projects.md) for task/project ownership,
  assignment, due dates, and Core API conventions.
- [M05 - Eventing + Relay](M05-eventing-relay.md) because notifications are driven from
  domain events and must stay consistent with the outbox/event model.
- A Core `comment` module. If comments were not completed in an earlier milestone, build
  the minimal comment and reaction module in this milestone before wiring comment-created
  notifications.

## Scope

**In:**

- Core `notification` module with:
  - notification records and read/mark-read operations;
  - notification preferences;
  - SSE hub and `/v1/notifications/stream` endpoint;
  - reminder and digest jobs;
  - Slack notification delivery adapter.
- Core `comment` module if not already present, including comments and reactions.
- Flyway migrations for comments, notifications, notification preferences, Slack
  notification preferences, and the ShedLock table.
- Frontend `notifications` feature with notification center, bell/badge, preference form,
  Pinia store, API client, and SSE subscription.
- Tests for notification creation, preferences, SSE behavior, scheduled jobs, Slack
  delivery stubbing, and comment-triggered notifications.

**Out:**

- Analytics dashboards and reporting projections; those belong to
  [M12 - Analytics + dashboard](M12-analytics-dashboard.md).
- Direct frontend calls to Relay or Nova. The frontend must continue to call **Core only**.
- Production email/Slack secret material in code. Use environment variables and local test
  stubs.

## Files to create or update

> Follow the repository's Flyway rule from `AGENTS.md`: append the next unused integer
> when adding migrations, and never edit an already-applied migration. The version labels
> below describe the reference milestone ordering; adjust only if earlier milestones have
> already consumed those version numbers.

### Core migrations

```text
apps/backend/src/main/resources/db/migration/V16__create_task_comments_tables.sql
  # task_comments, comment_reactions
apps/backend/src/main/resources/db/migration/V18__create_notifications_tables.sql
  # notifications, notification_preferences
apps/backend/src/main/resources/db/migration/V19__add_slack_notification_preferences.sql
apps/backend/src/main/resources/db/migration/V22__create_shedlock_table.sql
  # distributed locks for scheduled jobs
```

### Core comment module, if not already built

```text
apps/backend/src/main/java/com/taskmind/backend/comment/interfaces/rest/TaskCommentController.java
apps/backend/src/main/java/com/taskmind/backend/comment/interfaces/rest/CommentReactionController.java
apps/backend/src/main/java/com/taskmind/backend/comment/application/TaskCommentApplicationService.java
apps/backend/src/main/java/com/taskmind/backend/comment/application/CommentAuthorResolver.java
apps/backend/src/main/java/com/taskmind/backend/comment/domain/model/TaskComment.java
apps/backend/src/main/java/com/taskmind/backend/comment/domain/model/CommentReaction.java
apps/backend/src/main/java/com/taskmind/backend/comment/domain/repository/TaskCommentRepository.java
apps/backend/src/main/java/com/taskmind/backend/comment/domain/repository/CommentReactionRepository.java
apps/backend/src/main/java/com/taskmind/backend/comment/infrastructure/persistence/jpa/TaskCommentJpaEntity.java
apps/backend/src/main/java/com/taskmind/backend/comment/infrastructure/persistence/jpa/CommentReactionJpaEntity.java
apps/backend/src/main/java/com/taskmind/backend/comment/infrastructure/persistence/jpa/JpaTaskCommentRepository.java
apps/backend/src/main/java/com/taskmind/backend/comment/infrastructure/persistence/jpa/JpaCommentReactionRepository.java
```

### Core notification module

```text
apps/backend/src/main/java/com/taskmind/backend/notification/interfaces/rest/NotificationController.java
apps/backend/src/main/java/com/taskmind/backend/notification/interfaces/rest/NotificationPreferenceController.java
apps/backend/src/main/java/com/taskmind/backend/notification/application/NotificationApplicationService.java
apps/backend/src/main/java/com/taskmind/backend/notification/application/NotificationService.java
apps/backend/src/main/java/com/taskmind/backend/notification/application/NotificationSseHub.java
apps/backend/src/main/java/com/taskmind/backend/notification/application/NotificationReminderJob.java
apps/backend/src/main/java/com/taskmind/backend/notification/application/NotificationDigestJob.java
apps/backend/src/main/java/com/taskmind/backend/notification/domain/model/Notification.java
apps/backend/src/main/java/com/taskmind/backend/notification/domain/model/NotificationPreference.java
apps/backend/src/main/java/com/taskmind/backend/notification/domain/model/NotificationChannel.java
apps/backend/src/main/java/com/taskmind/backend/notification/domain/model/NotificationType.java
apps/backend/src/main/java/com/taskmind/backend/notification/domain/model/NotificationPreferenceProcess.java
apps/backend/src/main/java/com/taskmind/backend/notification/domain/repository/NotificationRepository.java
apps/backend/src/main/java/com/taskmind/backend/notification/domain/repository/NotificationPreferenceRepository.java
apps/backend/src/main/java/com/taskmind/backend/notification/infrastructure/persistence/jpa/NotificationJpaEntity.java
apps/backend/src/main/java/com/taskmind/backend/notification/infrastructure/persistence/jpa/NotificationPreferenceJpaEntity.java
apps/backend/src/main/java/com/taskmind/backend/notification/infrastructure/persistence/jpa/JpaNotificationRepository.java
apps/backend/src/main/java/com/taskmind/backend/notification/infrastructure/persistence/jpa/JpaNotificationPreferenceRepository.java
apps/backend/src/main/java/com/taskmind/backend/notification/infrastructure/slack/SlackNotificationSender.java
apps/backend/src/main/java/com/taskmind/backend/config/ShedLockConfig.java
apps/backend/src/main/java/com/taskmind/backend/config/SchedulingConfig.java
apps/backend/src/main/java/com/taskmind/backend/common/SlackNotifier.java
apps/backend/src/main/java/com/taskmind/backend/common/EmailSender.java
```

### Core API contract

```text
apps/backend/openapi.yaml
```

Keep `openapi.yaml` synchronized with all Core notification and comment request/response
shapes, including list, mark-read, preference, and SSE endpoint documentation.

### Frontend notifications feature

```text
apps/frontend/src/features/notifications/pages/NotificationCenter.vue
apps/frontend/src/features/notifications/components/NotificationBell.vue
apps/frontend/src/features/notifications/components/PreferencesForm.vue
apps/frontend/src/features/notifications/stores/notifications.ts
apps/frontend/src/features/notifications/api/notificationsApi.ts
```

Update the app shell/navigation so the notification bell is visible after login and the
notification center route is reachable by authenticated users.

### Tests

Create focused tests next to the owning modules. Suggested coverage:

```text
apps/backend/src/test/java/.../notification/NotificationControllerTest.java
apps/backend/src/test/java/.../notification/NotificationPreferenceControllerTest.java
apps/backend/src/test/java/.../notification/NotificationSseHubTest.java
apps/backend/src/test/java/.../notification/NotificationReminderJobTest.java
apps/backend/src/test/java/.../notification/NotificationDigestJobTest.java
apps/backend/src/test/java/.../comment/TaskCommentControllerTest.java
apps/frontend/src/features/notifications/**/__tests__/*.ts
```

## API requirements

All public notification endpoints live under Core `/v1/**` and require the authenticated
user, except routes already listed as public in `AGENTS.md`.

Minimum Core routes:

- `GET /v1/notifications` - page or list the current user's notifications.
- `GET /v1/notifications/unread-count` - return unread count for the bell badge.
- `PATCH /v1/notifications/{notificationId}/read` - mark one notification read.
- `PATCH /v1/notifications/read-all` - mark all current-user notifications read.
- `GET /v1/notifications/preferences` - read current-user notification preferences.
- `PUT /v1/notifications/preferences` - update current-user channel preferences.
- `GET /v1/notifications/stream` - SSE stream of new current-user notifications.

Comment routes, if introduced here, should remain Core task subresources and follow the
same auth/RBAC model as task access.

## Data and domain rules

- Notification ownership is per recipient. Users may only read, stream, or mutate their
  own notifications unless an explicit admin-only endpoint is added and protected.
- Notification types should cover at least task assignment, due-soon/reminder, and new
  comment activity.
- Preferences gate delivery by channel: in-app, email digest, and Slack.
- In-app notifications should still be persisted even when optional external channels are
  disabled, unless the user's in-app preference is explicitly disabled.
- Store Slack delivery configuration in preferences/configuration tables and environment
  variables; never store Slack secrets in source-controlled code.
- Use optimistic locking on mutable JPA entities, consistent with the repository-wide
  entity rule.

## Key design notes

- The SSE stream is exposed at `GET /v1/notifications/stream` through
  `NotificationSseHub`. Configure gateway/ALB/nginx behavior for long-lived connections
  in the AWS/local infrastructure milestone instead of bypassing Core.
- Scheduled reminder and digest jobs must use **ShedLock** and the `shedlock` table so
  only one service instance fires each job in a multi-instance deployment.
- Notifications are produced from Core domain events such as assignment changes,
  due-soon/reminder triggers, and new comments.
- Slack delivery mode is configurable with an environment-backed property such as
  `TASKMIND_NOTIFICATIONS_SLACK_DELIVERY`; tests should stub the sender.
- Add a feature flag such as `taskmind.notifications.enabled` so the module can be
  disabled during local troubleshooting or staged rollout.
- Keep frontend notification APIs pointed at Core only. Do not introduce Relay/Nova calls
  from the browser.

## Acceptance criteria

- [ ] Assignment, due/reminder, and comment events create in-app notifications for the
      correct recipients.
- [ ] SSE pushes new notifications live to the authenticated user's browser session.
- [ ] Mark-read and mark-all-read operations update unread counts and are scoped to the
      current user.
- [ ] Preferences control in-app, email digest, and Slack delivery channels.
- [ ] Slack delivery is exercised through a stub/fake sender in tests.
- [ ] Reminder and digest jobs run under ShedLock and only fire once per schedule in a
      multi-instance deployment.
- [ ] Notification center UI, bell badge, and preferences UI work for authenticated users.
- [ ] Core `openapi.yaml` matches the implemented notification/comment contracts.

## Verification

```bash
cd apps/backend && mvn -q -Dtest='*Notification*,*Comment*' test
make vibe-verify
# Browser E2E: log in with the superadmin bypass, assign a task, confirm the bell badge
# and notification center update through SSE, mark the notification read, and edit
# notification preferences.
```

## Definition of Done

In-app notifications, SSE live updates, reminder/digest jobs, Slack delivery, user
preferences, and the comments module are present and covered by tests. `make vibe-verify`
is green, Core `openapi.yaml` is synchronized, and browser E2E confirms live notification
behavior through the frontend.
