# TESTME — Validate Docker (512MB / 1 vCPU)

This guide validates the app running **inside Docker** with enforced limits using:
- `docker-compose.yml` (base services: api + postgres)
- `docker-compose.limits.yml` (overlay: constrain `api`, add cAdvisor)

## 0) Prerequisites

- Docker Engine + `docker compose` plugin
- Ports free on your machine: `8080` (api), `8081` (cAdvisor), `5432` (Postgres)

From repo root:

- `pwd` should be the folder containing `docker-compose.yml`.

## 1) Start the constrained environment

Run:

- `docker compose -f docker-compose.yml -f docker-compose.limits.yml up --build`

What this does:
- Constrains **api** to `512MB` + `1 vCPU`
- Starts Postgres (not constrained)
- Starts cAdvisor at `http://localhost:8081`

Stop:
- `docker compose -f docker-compose.yml -f docker-compose.limits.yml down -v`

## 2) Verify the resource limits are applied

### Option A: docker stats

- `docker stats`

Confirm:
- `api` memory stays under ~`512MiB`
- CPU is capped around `1.0` CPU

### Option B: cAdvisor (recommended)

Open:
- `http://localhost:8081`

Look for the `api` container and confirm memory/CPU graphs.

## 3) Verify the app is up

### 3.1 Swagger should be accessible without auth

- `curl -i http://localhost:8080/swagger-ui.html`

Expect: `HTTP/1.1 200` (or a 3xx redirect to swagger assets).

### 3.2 Protected endpoints should require JWT

- `curl -i http://localhost:8080/api/tenant/current`

Expect: `401 Unauthorized`.

### 3.3 Actuator health should be accessible without auth

- `curl -i http://localhost:8080/actuator/health`

Expect: `200 OK`.

## 4) Login (no Python): access token + refresh token

This repo includes a built-in login flow:
- `POST /api/public/auth/login` → returns `accessToken` + `refreshToken`
- `POST /api/public/auth/refresh` → rotates refresh token + returns new access token

To log in, you must first create a user identity.

### 4.1 Quick-start: create an account via TRIAL signup (no payment)

This creates:
- the user identity (email + password)
- the tenant + OWNER membership
- a TRIAL subscription (if missing)

- `curl -sS -X POST http://localhost:8080/api/public/signup/checkout \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId":"tenant-a",
    "tenantDisplayName":"Tenant A",
    "ownerEmail":"owner@tenant-a.local",
    "ownerPassword":"ChangeMe!123",
    "planCode":"starter",
    "interval":"MONTHLY",
    "mode":"TRIAL"
  }' | jq .`

### 4.2 Login and export tokens

- `curl -sS -X POST http://localhost:8080/api/public/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"owner@tenant-a.local","password":"ChangeMe!123"}' | jq .`

Export (copy values from the JSON response):
- `export ACCESS_TOKEN='<ACCESS_TOKEN>'`
- `export REFRESH_TOKEN='<REFRESH_TOKEN>'`

Refresh (when access token expires):
- `curl -sS -X POST http://localhost:8080/api/public/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"'"$REFRESH_TOKEN"'"}' | jq .`

### 4.3 Verify tenant resolution

Header wins over claim, so just pass `X-Tenant-Id`:

- `curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" -H "X-Tenant-Id: tenant-a" http://localhost:8080/api/tenant/current | jq .`

Expect: `tenantId` is `tenant-a`.

## 5) Validate tenant isolation (COLUMN mode)

This section assumes you are logged in as the same user, and you have memberships in both tenants.

Create a second tenant (same email + password, different tenantId):

- `curl -sS -X POST http://localhost:8080/api/public/signup/checkout \
  -H 'Content-Type: application/json' \
  -d '{
    "tenantId":"tenant-b",
    "tenantDisplayName":"Tenant B",
    "ownerEmail":"owner@tenant-a.local",
    "ownerPassword":"ChangeMe!123",
    "planCode":"starter",
    "interval":"MONTHLY",
    "mode":"TRIAL"
  }' | jq .`

### 5.1 Create a user in tenant-a

- `curl -sS -H "Authorization: Bearer $ACCESS_TOKEN" -H "X-Tenant-Id: tenant-a" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","displayName":"Alice"}' \
  http://localhost:8080/api/users | jq .`

### 5.2 Query in tenant-a (should be found)

- `curl -i -H "Authorization: Bearer $ACCESS_TOKEN" -H "X-Tenant-Id: tenant-a" \
  "http://localhost:8080/api/users?email=alice@example.com"`

Expect: `200 OK`.

### 5.3 Query in tenant-b (should NOT be found)

- `curl -i -H "Authorization: Bearer $ACCESS_TOKEN" -H "X-Tenant-Id: tenant-b" \
  "http://localhost:8080/api/users?email=alice@example.com"`

Expect: `404 Not Found`.

### 5.4 Create same email in tenant-b (should succeed)

- `curl -i -H "Authorization: Bearer $ACCESS_TOKEN" -H "X-Tenant-Id: tenant-b" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","displayName":"Alice B"}' \
  http://localhost:8080/api/users`

Expect: `200 OK`.

## 6) Inspect JVM flags and native memory (optional)

### 6.1 Confirm JAVA_TOOL_OPTIONS inside the running container

- `docker compose -f docker-compose.yml -f docker-compose.limits.yml exec api sh -lc 'echo "$JAVA_TOOL_OPTIONS"'`

### 6.2 Native Memory Tracking (NMT)

The image enables `-XX:NativeMemoryTracking=summary`.

If you want a point-in-time view:
- `docker compose -f docker-compose.yml -f docker-compose.limits.yml exec api sh -lc '/opt/java/bin/jcmd 1 VM.native_memory summary'`

If `jcmd` is missing (older image build), rebuild the image after pulling latest Dockerfile changes.

## 7) Common issues

- **401 on everything**: expected; only Swagger is public. Use the login steps in section 4.
- **Port conflicts**: change host ports in compose if `8080`, `8081`, or `5432` are already in use.
- **Memory usage too high**: most of the baseline is heap + metaspace + threads (native stack). This repo defaults to `-Xms/-Xmx 300m` now; you can lower further (e.g. `280m`) but expect more GC/latency. Disabling NMT (`-XX:NativeMemoryTracking=off`) can also reclaim a few MiBs when you’re not actively inspecting memory.
- **Thread pools**: in [docker-compose.limits.yml](docker-compose.limits.yml) the constrained profile lowers Tomcat thread limits mainly as a concurrency/backpressure knob for 1 vCPU. If you reduce `SERVER_TOMCAT_THREADS_MAX` too far (e.g. `5`), expect throughput to drop under concurrent load.
- **Tomcat connections**: constrained mode also limits `SERVER_TOMCAT_MAX_CONNECTIONS` and `SERVER_TOMCAT_ACCEPT_COUNT` to apply backpressure early and avoid too many concurrent sockets/requests in a small container.
- **Virtual threads**: constrained mode sets `SPRING_THREADS_VIRTUAL_ENABLED=true`. Spring Boot (3.2+) also wires embedded Tomcat to use a virtual-thread executor when this is enabled.
- **GC choice**: the image defaults to `-XX:+UseSerialGC` for a smaller/simpler footprint on 1 vCPU. If you see unacceptable pauses under load, switch back to G1 by overriding `JAVA_TOOL_OPTIONS` (remove `UseSerialGC`, add `-XX:+UseG1GC`).
- **OutOfMemoryError: Metaspace**: increase `-XX:MaxMetaspaceSize` (or remove the cap), and consider lowering `-Xmx` a bit to keep the total under `512MB`.
- **StackOverflowError**: increase `-Xss` (thread stack) from `320k` to `384k` or `512k`.
- **Don’t set `-Xss` too low**: values below ~`128k` are likely to cause hard-to-debug failures and are not worth the tiny memory win.
- **Docker build fails at jlink**: ensure you rebuilt after Dockerfile changes (`docker compose ... up --build`) and that the build stage uses a full JDK image (it does: `eclipse-temurin:21-jdk-alpine`).
- **NoClassDefFoundError: java/beans/PropertyEditorSupport**: your minimized runtime is missing `java.desktop`. Rebuild after pulling Dockerfile changes.

## 8) E2E flow (Swagger + Virtual payments)

This section is a guided manual E2E that matches the micro‑SaaS happy path:

1) Select a plan
2) Pay virtually
3) Receive confirmation (webhook processed)
4) Tenant onboarding becomes active (tenant + OWNER membership created)
5) “Log in” (mint a dev JWT)
6) Create TODO list/items, list items, complete, delete

### 8.1 Enable the Virtual payments provider

The Virtual provider is useful to exercise a full checkout → webhook → subscription activation flow without Stripe.

Option A (recommended for manual testing): add these env vars under the `api:` service in [docker-compose.yml](docker-compose.yml):

- `APP_PAYMENTS_PROVIDER=VIRTUAL`
- `APP_PAYMENTS_WEBHOOKS_SECRET=test-webhook-secret`

Then restart the stack:

- `docker compose -f docker-compose.yml -f docker-compose.limits.yml down -v`
- `docker compose -f docker-compose.yml -f docker-compose.limits.yml up --build`

Option B: run without Virtual provider and skip the webhook steps.

### 8.2 Select a plan (public)

Plan catalog is public (no JWT):

- `GET /api/public/plans`

Pick one of: `starter`, `pro`, `enterprise`.

### 8.3 Start checkout (public signup)

This endpoint is public and exists specifically so you can do the happy-path in the order:
plan → payment → onboarding → login.

1) `POST /api/public/signup/checkout`
  - Body example:
    ```json
    {
      "tenantId": "t-e2e",
      "tenantDisplayName": "E2E Tenant",
      "ownerEmail": "owner@e2e.local",
      "ownerPassword": "ChangeMe!123",
      "planCode": "starter",
      "interval": "MONTHLY",
      "mode": "PAID"
    }
    ```
  - Expect: `provider = virtual`, a `sessionId`, and a `url`.
  - Save `TENANT_ID` and `SESSION_ID`.

### 8.4 Send the payment confirmation webhook (Virtual)

Webhook endpoint is public (no JWT), but it requires header `X-Webhook-Secret`.

Depending on Swagger UI configuration, custom headers may not be available for this endpoint.
If Swagger doesn’t let you set `X-Webhook-Secret`, use curl:

- `curl -sS -X POST http://localhost:8080/api/billing/webhooks/virtual \
  -H 'Content-Type: application/json' \
  -H 'X-Webhook-Secret: test-webhook-secret' \
  -d '{"eventId":"evt_1","type":"checkout.session.completed","sessionId":"<SESSION_ID>"}' | jq .`

Expect: `{ "status": "ok" }`.

### 8.5 Receive confirmation (public status)

- `GET /api/public/signup/sessions/virtual/<SESSION_ID>`

Expect:
- `checkoutStatus = COMPLETED`
- `signupStatus = COMPLETED`
- `tenantOnboarded = true`

### 8.6 “Log in” (mint a dev JWT)

- Open `http://localhost:8080/swagger-ui.html`

Login with the built-in endpoint:

- `POST /api/public/auth/login`
  - Body: `{ "email": "owner@e2e.local", "password": "ChangeMe!123" }`
  - Copy `accessToken`.

In Swagger UI, click **Authorize** and paste:

- `Bearer <accessToken>`

### 8.7 Verify onboarding is active

Now that payment is confirmed, the tenant + OWNER membership exists.

1) `GET /api/tenants/mine`
  - Expect: includes tenant `t-e2e` with role `OWNER`.

### 8.8 Confirm tenant context resolution

Tenant resolution uses header `X-Tenant-Id` (preferred) or JWT claim `tenant_id`.

Call:

- `GET /api/tenant/current`
  - Set header: `X-Tenant-Id: t-e2e`

Expect: `tenantId = "t-e2e"`.

### 8.9 Todo lists & items

All TODO endpoints require a tenant header.

1) `POST /api/todos/lists`
  - Header: `X-Tenant-Id: t-e2e`
  - Body: `{ "name": "My first list" }`
  - Save the returned `id` as `LIST_ID`.

2) `POST /api/todos/lists/{listId}/items`
  - Header: `X-Tenant-Id: t-e2e`
  - Path: `listId = LIST_ID`
  - Body: `{ "title": "First task", "description": "Try swagger E2E" }`
  - Save the returned `id` as `ITEM_ID`.

3) `PATCH /api/todos/items/{itemId}`
  - Header: `X-Tenant-Id: t-e2e`
  - Path: `itemId = ITEM_ID`
  - Body example: `{ "completed": true }`
  - Expect: item marked completed.

4) `DELETE /api/todos/items/{itemId}`
  - Header: `X-Tenant-Id: t-e2e`
  - Path: `itemId = ITEM_ID`
  - Expect: `204 No Content`.

### 8.10 (Optional) Verify subscription is ACTIVE

- `GET /api/subscription/current`
  - Header: `X-Tenant-Id: t-e2e`

Expect:
- `status = ACTIVE`
- `currentPeriodEndsAt` is set

### 8.11 (Optional) replay the same webhook (idempotency)

Re-send the exact same payload with `eventId="evt_1"`.
Expect: still `ok`, and subscription remains `ACTIVE` (no duplicate processing).

## 9) Join existing tenant via invite (E2E)

Goal:
- Accept invite → (create user if needed) → membership created with pre-set role → login → first TODO

### 9.1 Create an invite (as an OWNER/ADMIN)

1) Login as the tenant owner (section 4 or 8.6)
2) Create an invite:

- `POST /api/tenants/{tenantId}/invites`
  - Header: `X-Tenant-Id: t-e2e`
  - Body example:
    ```json
    {
      "email": "member@e2e.local",
      "role": "MEMBER"
    }
    ```

Save the returned `token` as `INVITE_TOKEN`.

### 9.2 Accept the invite (public)

- `POST /api/public/invites/accept`
  - Body example:
    ```json
    {
      "token": "<INVITE_TOKEN>",
      "email": "member@e2e.local",
      "password": "ChangeMe!123"
    }
    ```

Expect:
- membership details (`tenantId`, `role`, ...)
- `accessToken` + `refreshToken`

### 9.3 Use the returned access token to create a TODO

In Swagger UI, click **Authorize** and paste:
- `Bearer <accessToken>`

Then:
- `POST /api/todos/lists` (Header `X-Tenant-Id: t-e2e`)
- `POST /api/todos/lists/{listId}/items`
- `PATCH /api/todos/items/{itemId}`
- `DELETE /api/todos/items/{itemId}`
