# TaskMind Monorepo

TaskMind is an AI-centered task manager built as a monorepo:

- **Frontend**: Vue 3 + Vite (`apps/frontend`)
- **Backend**: Java 21 + Spring Boot (`apps/backend`)

## Project Structure

```text
apps/
  frontend/   # Vue web app
  backend/    # Spring Boot API
```

## Prerequisites

- Node.js 20+
- npm 10+
- Java 21
- Maven 3.9+

## Quick Start

### 1) Frontend

```bash
cd apps/frontend
npm install
npm run dev
```

Frontend runs by default on `http://localhost:5173`.

### 2) Backend

```bash
cd apps/backend
mvn spring-boot:run
```

Backend runs by default on `http://localhost:8080`.

## Initial API

- `GET /api/health` → `{ "status": "ok", "service": "taskmind-backend" }`

## API Contract Draft

- OpenAPI spec for the current task endpoints: `apps/backend/openapi.yaml`

## Next Steps

- Add authentication + user accounts
- Expand task APIs from in-memory scaffold to PostgreSQL-backed APIs
- Add AI-assisted task prioritization/scheduling
- Connect frontend with backend API
