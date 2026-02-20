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

### Slice 2 — Identity & tenant membership (done)
- Added stable principal helper (subject + email from JWT).
- Added control-plane tenant registration + memberships + invite flow.
- API:
  - `POST /api/tenants` create tenant (creator becomes `OWNER`)
  - `GET /api/tenants/mine` list my memberships
  - `POST /api/tenants/{tenantId}/invites` create invite (requires `OWNER`/`ADMIN`)
  - `POST /api/tenants/invites/accept` accept invite by token

### Slice 3 — Authorization: roles + permissions
- Add a small permission component (`tenantPermission`) usable from `@PreAuthorize`.
- Implement mapping rules (in order):
  - `SCOPE_admin` / `ROLE_ADMIN` => allow everything
  - `SCOPE_{permissionCode}` => allow
  - fallback: control-plane membership role => allow based on persisted tenant roles/permissions
- Apply to endpoints (initial rollout):
  - TODO API (`todo.read`, `todo.write`)
  - Users API (`user.read`, `user.write`)
  - Subscription API (`subscription.read`, `subscription.write`)
  - Billing API (`billing.write`)
  - Tenant invite API (`tenant.invite.write`)

- Organization access API (tenant-scoped):
  - `GET /api/organization-access/roles` (scope: `authz.read`)
  - `POST /api/organization-access/roles` (scope: `authz.write`)
  - `PUT /api/organization-access/roles/{roleName}/permissions/{permissionCode}` (scope: `authz.write`)
  - `DELETE /api/organization-access/roles/{roleName}/permissions/{permissionCode}` (scope: `authz.write`)
  - `GET /api/organization-access/permissions` (scope: `authz.read`)

### Slice 4 — Subscriptions, plan features & pricing
- Subscription is tenant-scoped and drives which plan features are active.
- Plans are global catalog entries with plan feature codes (e.g. `plan.users.manage`).
- Add pricing as a first-class model (`PlanPrice`) and store provider-specific price identifiers.

Notes:
- `PlanPrice` supports multiple intervals (monthly/yearly) and a provider price-id map.
- Billing checkout takes `planCode` (+ optional interval) and resolves the provider price id before delegating to the `PaymentGateway`.

### Slice 5 — Payments provider abstraction
- Keep `PaymentGateway` as the stable interface.
- Add a provider registry (Stripe, Paddle, etc.) and minimal webhook processing.
- Add idempotency and tenant-aware reconciliation.

Next payment lifecycle steps:
- Persist a "pending checkout" record keyed by tenant + provider session id.
- On webhook success, transition tenant `Subscription` to `ACTIVE` and set `currentPeriodEndsAt` based on the purchased interval.
- Store provider customer/subscription ids to support upgrades/downgrades and cancellation.

### Slice 6 — Hardening multi-tenancy modes
- Validate behavior in all modes (`COLUMN`, `SCHEMA`, `DATABASE`).
- Add focused integration tests for:
  - schema switching correctness,
  - database routing correctness,
  - RLS policy enforcement (Postgres-only profile).
