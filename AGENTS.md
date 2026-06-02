# TaskMind Agent Instructions

This repository is a TaskMind monorepo. Before making product, architecture, or cross-service changes, read the product and architecture overview:

1. Start with [`docs/build-kit/00-overview.md`](docs/build-kit/00-overview.md) for the product scope, service ownership, data flow, and target architecture.
2. Keep service boundaries strict: the frontend talks only to Core; Core owns business state; Relay projects analytics/search read models; Nova handles LLM/chat runtime.
3. Prefer changing the owner service for a concern rather than duplicating state or behavior across services.
4. When adding or changing UI behavior, verify with the frontend checks and, when runnable, browser/E2E validation.
5. When adding or changing backend behavior, verify with Maven tests/checks from `apps/backend`.

## Current repository shape

The current checked-in implementation contains:

- `apps/frontend`: Vue 3 + Vite SPA.
- `apps/backend`: Spring Boot Core API.
- `skills/`: local agent skills and implementation maps.
- `docs/`: product and implementation documentation.

The architecture overview also documents planned services (`apps/relay`, `apps/ai`) and shared Java libraries that may not exist yet in this branch.
