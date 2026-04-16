# Backend Implementation Map

Use this map to quickly route a request to the right backend files.

## API layer

- `apps/backend/src/main/java/com/taskmind/backend/task/interfaces/rest/TaskController.java`
- `apps/backend/src/main/java/com/taskmind/backend/task/interfaces/rest/PlanningController.java`
- `apps/backend/src/main/java/com/taskmind/backend/project/interfaces/rest/ProjectController.java`
- `apps/backend/src/main/java/com/taskmind/backend/project/interfaces/rest/ProjectMembershipController.java`
- DTO packages:
  - `.../task/interfaces/rest/dto/*`
  - `.../project/interfaces/rest/dto/*`

## Application layer

- Task orchestration:
  - `.../task/application/TaskApplicationService.java`
  - `.../task/application/CreateTaskCommand.java`
  - `.../task/application/UpdateTaskCommand.java`
- Project orchestration:
  - `.../project/application/ProjectApplicationService.java`
  - `.../project/application/ProjectMembershipApplicationService.java`
  - `.../project/application/*Command.java`

## Domain layer

- Task domain:
  - `.../task/domain/model/*`
  - `.../task/domain/repository/TaskRepository.java`
- Project domain:
  - `.../project/domain/model/*`
  - `.../project/domain/repository/*`

## Infrastructure / persistence

- Task JPA adapters:
  - `.../task/infrastructure/persistence/jpa/*`
- Project JPA adapters:
  - `.../project/infrastructure/persistence/jpa/*`

## Database migrations

- `apps/backend/src/main/resources/db/migration/V1__create_tasks_table.sql`
- `apps/backend/src/main/resources/db/migration/V2__create_projects_table.sql`
- `apps/backend/src/main/resources/db/migration/V3__create_project_memberships_table.sql`

Add future migrations using the same Flyway naming convention and increment the version.

## Test focus points

- Task controller tests:
  - `apps/backend/src/test/java/com/taskmind/backend/task/interfaces/rest/TaskControllerTest.java`
- Project membership controller tests:
  - `apps/backend/src/test/java/com/taskmind/backend/project/interfaces/rest/ProjectMembershipControllerTest.java`

When adding endpoints, mirror these test patterns and naming conventions.
