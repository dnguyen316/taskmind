# M01 — Core Foundations

## Objective

Give Core its backbone: Flyway-owned persistence for tasks, projects, and auth tables; a
stateless JWT resource-server security layer with public auth routes; the profile-gated
E2E auth bypass with a seeded super-admin; RFC 7807 error handling; and request-context
logging. After M01, a user can sign up, verify OTP, log in, receive JWTs, refresh a
session, and read their own authenticated user profile.

## Depends on

[M00](M00-bootstrap.md).

## Scope

**In:** Core-only foundation work:

- Flyway migrations for the reference V1-V5-equivalent Core schema slice:
  - tasks
  - projects
  - project memberships
  - optimistic-lock `version` columns
  - auth/authz tables
- Auth module:
  - signup
  - email OTP verification
  - login
  - refresh-token sessions
  - logout
  - current-user profile
  - RBAC seed data
- Security module:
  - stateless JWT resource server
  - public health/auth routes
  - authorization guardrails for `/v1/**`
  - JWT claim-to-authority conversion
- Common module:
  - RFC 7807 `ProblemDetail` responses
  - request-context filter
  - conflict exception support
- Profile-specific configuration for local, staging, test, and prod.
- E2E bypass seeder for non-prod/test use.

**Out:** Task/project CRUD business endpoints, scheduler, events/outbox, Relay, Nova,
frontend auth UI, OAuth provider polish, email delivery integrations beyond the local/test
OTP dispatch abstraction.

## Files to create

All paths in this section are under `apps/backend/src/main` unless a different root is
shown.

```text
# Migrations (apps/backend/src/main/resources/db/migration/)
V1__create_tasks_table.sql
V2__create_projects_table.sql
V3__create_project_memberships_table.sql
V4__add_optimistic_lock_version_columns.sql
V5__create_auth_authz_tables.sql          # users, identities, roles, permissions,
                                          # user_roles, role_permissions, sessions,
                                          # otp_challenges
V6__add_user_display_name_and_seed_roles.sql
                                          # display_name + seed MEMBER/MANAGER/ADMIN roles
V7__seed_rbac_permissions.sql             # seed permissions and role-permission grants

# auth module (apps/backend/src/main/java/com/taskmind/backend/auth/)
interfaces/rest/AuthController.java        # /v1/auth/**
application/AuthApplicationService.java
application/JpaAuthApplicationService.java
application/command/SignupEmailCommand.java
application/command/VerifyOtpCommand.java
application/command/LoginCommand.java
application/command/RefreshTokenCommand.java
application/command/LogoutCommand.java
domain/TokenService.java
domain/AuthException.java
domain/EmailOtpDispatchService.java
infrastructure/security/JwtTokenService.java
infrastructure/security/JwtConfiguration.java
infrastructure/oauth/GoogleIdTokenVerifier.java
infrastructure/persistence/jpa/UserJpaEntity.java
infrastructure/persistence/jpa/IdentityJpaEntity.java
infrastructure/persistence/jpa/RoleJpaEntity.java
infrastructure/persistence/jpa/PermissionJpaEntity.java
infrastructure/persistence/jpa/SessionJpaEntity.java
infrastructure/persistence/jpa/OtpChallengeJpaEntity.java
infrastructure/persistence/jpa/UserJpaRepository.java
infrastructure/persistence/jpa/IdentityJpaRepository.java
infrastructure/persistence/jpa/RoleJpaRepository.java
infrastructure/persistence/jpa/PermissionJpaRepository.java
infrastructure/persistence/jpa/SessionJpaRepository.java
infrastructure/persistence/jpa/OtpChallengeJpaRepository.java
infrastructure/otp/JpaEmailOtpService.java
infrastructure/e2e/AuthE2eBypass.java
infrastructure/e2e/E2eSuperAdminSeeder.java

# security module (apps/backend/src/main/java/com/taskmind/backend/security/)
SecurityConfig.java
ApiSecurityAuthorization.java
JwtClaimAuthenticationConverter.java
AuthenticatedUserResolver.java

# common module (apps/backend/src/main/java/com/taskmind/backend/common/)
GlobalExceptionHandler.java                # RFC 7807 ProblemDetail
RequestContextFilter.java
ConflictException.java

# config / profiles (apps/backend/src/main/resources/)
application.properties                     # taskmind.auth.jwt.*, OTP, e2e-bypass defaults
application-local.properties               # e2e-bypass on
application-staging.properties             # e2e-bypass off
application-e2e.properties                 # isolated browser E2E opt-in; e2e-bypass on
application-prod.properties                # e2e-bypass off; require TASKMIND_JWT_SECRET

# test resources
apps/backend/src/test/resources/application-test.properties
                                          # H2 PostgreSQL mode, Flyway enabled,
                                          # bypass on, rate limiting off,
                                          # external services excluded
apps/backend/src/test/java/com/taskmind/backend/security/TestSecurityConfig.java
```

Add focused tests alongside the new modules, including auth controller/application tests,
security-route tests, profile guard tests, Flyway migration validation, and repository
persistence tests for the auth/session entities.

## Key design notes

- Core remains the only owner of users, auth, RBAC, sessions, tasks, projects, and project
  membership persistence.
- Schema is owned by Flyway. Hibernate must validate the schema rather than create or
  mutate it (`spring.jpa.hibernate.ddl-auto=validate`).
- Migrations are appended in integer order and must be compatible with PostgreSQL and H2
  running in PostgreSQL mode because tests run Flyway.
- The stateless JWT resource server uses HS256. Public routes are `/api/health` and the
  auth flows; everything else under `/v1/**` requires authentication; all other routes are
  denied.
- JWT claims map to authorities through `JwtClaimAuthenticationConverter`; support the
  `roles`, `authorities`, and `scope` claim shapes described by the Core API contract.
- Sessions store hashed refresh tokens. Refresh rotates the token, updates the session,
  and invalidates the previous refresh token.
- E2E bypass is enabled only for `local`, `test`, and isolated browser E2E runs that
  activate the dedicated `e2e` profile; generic `staging` keeps it disabled, and the app
  must fail startup if the bypass is forced on without an allowed profile.
- E2E seeding creates `superadmin@taskmind.local` with password `1` and OTP `1` for the
  documented local/test/e2e bypass flow.
- Errors return RFC 7807 `ProblemDetail` bodies. Unauthenticated requests return `401`
  `ProblemDetail` responses.
- Keep `apps/backend/openapi.yaml` in sync with any Core request or response DTOs added in
  this milestone.

## Acceptance criteria

- [ ] Flyway migrates cleanly on PostgreSQL and H2 test configuration.
- [ ] `POST /v1/auth/signup/email` creates a pending user/identity and OTP challenge.
- [ ] Signup OTP flow succeeds through `POST /v1/auth/verify` and activates the user.
- [ ] `POST /v1/auth/login` returns access and refresh tokens for an active user.
- [ ] `/v1/auth/token/refresh` rotates refresh tokens and returns a new token pair.
- [ ] `POST /v1/auth/logout` invalidates the active refresh-token session.
- [ ] `GET /v1/auth/me` requires authentication and returns the authenticated user.
- [ ] Unauthenticated `GET /v1/auth/me` returns `401` with a `ProblemDetail` body.
- [ ] JWT authorization protects every non-public `/v1/**` route and denies all routes
  outside the explicit public/authenticated surface.
- [ ] The application fails startup with the `prod` profile if E2E bypass is enabled.
- [ ] Super-admin bypass login works in the `local` profile using
  `superadmin@taskmind.local` / password `1` / OTP `1`.
- [ ] `make vibe-verify` passes end to end.

## Verification

```bash
# from repo root
mvn -q -Dtest='*Auth*' test
make vibe-verify

# manual local smoke, after M00 infra/run targets exist
make run-backend
# then exercise signup/login/refresh/logout and the superadmin bypass against localhost:8080
```

## Definition of Done

Auth, security, and persistence foundations are green under `make vibe-verify`; Core
starts with local/test/e2e bypass settings, keeps generic staging bypass disabled, rejects an enabled disallowed-profile bypass at startup,
and allows the seeded super-admin to authenticate locally.
