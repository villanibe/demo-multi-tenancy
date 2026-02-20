# Roadmap (incremental refactor plan)

This repository is a **micro‑SaaS multi‑tenant blueprint**. The goal is to evolve it incrementally (small safe slices) into a production-ready platform while keeping tenant isolation strong and keeping tenancy strategy **switchable**.

## Tenancy strategies (switchable)

Configured via `app.tenancy.mode`:

- `COLUMN` (default): single schema + `tenant_id` discriminator column on tenant-scoped tables.
  - Application-level guardrails: `TenantScopedEntity` auto-assigns `tenant_id` on persist.
  - Query isolation: repositories should always filter by `tenantId` for reads.
  - Stronger isolation option: enable **Postgres RLS** (see below).

- `SCHEMA`: same DB, separate schema per tenant.
  - Hibernate multi-tenancy uses `SCHEMA` and switches the current schema per request.
  - Operational requirement: schemas must exist and be managed/migrated.

- `DATABASE`: separate database (or DataSource) per tenant.
  - Hibernate multi-tenancy uses `DATABASE` and routes connections per tenant.
  - Operational requirement: tenant DS map must be configured and provisioned.

## Reinforcing isolation with Postgres RLS

When using Postgres, you can turn on RLS by setting:

- `app.tenancy.rls.enabled=true`
- `app.tenancy.rls.postgres.settingName=app.tenant_id`

The platform sets the tenant id on each DB connection using a session setting (e.g. `SET app.tenant_id = 'tenant-a'`). Then tables can enforce tenant filtering with RLS policies.

Notes:
- RLS is best paired with `COLUMN` mode.
- Keep application-level filtering even with RLS (defense in depth).

## First vertical slice: TODO Lists (done)

Feature: tenant-scoped TODO lists and items.

- Data model: `todo_lists`, `todo_items` (both tenant-scoped)
- API (JWT required):
  - `POST /api/todos/lists` (scope: `todo.write`)
  - `GET /api/todos/lists` (scope: `todo.read`)
  - `POST /api/todos/lists/{listId}/items` (scope: `todo.write`)
  - `GET /api/todos/lists/{listId}/items` (scope: `todo.read`)
  - `PATCH /api/todos/items/{itemId}` (scope: `todo.write`)
  - `DELETE /api/todos/items/{itemId}` (scope: `todo.write`)

## Next slices (suggested order)

### Slice 2 — Identity & tenant membership
- Add **tenant membership** model: user belongs to one or more tenants.
- Add endpoints for tenant onboarding and membership invites.
- Introduce a stable `principal` representation (subject id, email, tenant memberships).

### Slice 3 — Authorization: roles + permissions
- Standardize on one approach:
  - OAuth2 scopes (API-friendly), and/or
  - tenant-scoped roles/permissions persisted in DB.
- Add a small “permission service” that maps JWT claims to platform permissions.

### Slice 4 — Subscriptions & entitlements
- Ensure subscription is tenant-scoped and drives entitlements.
- Add plan/feature flags and a tenant entitlement check used by controllers/services.

### Slice 5 — Payments provider abstraction
- Keep `PaymentGateway` as the stable interface.
- Add a provider registry (Stripe, Paddle, etc.) and minimal webhook processing.
- Add idempotency and tenant-aware reconciliation.

### Slice 6 — Hardening multi-tenancy modes
- Validate behavior in all modes (`COLUMN`, `SCHEMA`, `DATABASE`).
- Add focused integration tests for:
  - schema switching correctness,
  - database routing correctness,
  - RLS policy enforcement (Postgres-only profile).
