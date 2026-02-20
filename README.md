# demo-multi-tenancy (0.0.2)

Blueprint service for a micro‑SaaS platform with multi‑tenancy, JWT/OAuth2 resource server security, and core building blocks (users, subscriptions, metering, payments gateway).

## Requirements

- Java 21
- Docker (optional)

## Run locally (Java)

- Start Postgres (or use Docker Compose below)
- Run the app:
  - `./mvnw spring-boot:run`

App listens on `http://localhost:8080`.

## Run with Docker Compose

- Build and start services:
  - `docker compose up --build`

Optional resource-limited variant:
- `docker compose -f docker-compose.yml -f docker-compose.limits.yml up --build`

Note: `docker compose` enforces `mem_limit`/`cpus` directly; if you rely on `deploy.resources` you may need `docker compose --compatibility`.

When using the limits overlay, cAdvisor is also started for monitoring:
- `http://localhost:8081`

## HikariCP sizing (512MB / 1 vCPU)

Spring Boot uses HikariCP by default. This project pins conservative defaults in application config:
- `maximumPoolSize=4`, `minimumIdle=0`
- `connectionTimeout=2000ms`, `validationTimeout=1000ms`
- `idleTimeout=60s`, `maxLifetime=25m`

If you run in `DATABASE` tenancy mode, remember that each tenant DataSource can create its own pool, so the effective maximum connections is roughly `poolSize * tenantCount`.

### JVM memory in 512MB containers

The Docker image sets `JAVA_TOOL_OPTIONS` defaults tuned for small containers:
- Fixed heap: `-Xms280m -Xmx280m` (keeps memory predictable)
- Capped non-heap: `-XX:MaxMetaspaceSize=128m`, `-XX:ReservedCodeCacheSize=32m`, `-Xss256k`, `-XX:MaxDirectMemorySize=64m`
- GC: `-XX:+UseSerialGC` (good default for 1 vCPU / small footprint)
- Native Memory Tracking: `-XX:NativeMemoryTracking=summary` (useful for diagnosing native memory; has overhead)
- Fast-fail on OOM: `-XX:+ExitOnOutOfMemoryError`

To inspect native memory (when troubleshooting), exec into the container and run:
- `jcmd 1 VM.native_memory summary`

Override example:
- `docker compose run -e JAVA_TOOL_OPTIONS="..." api`

## Multi-tenancy

Tenant is resolved per request from:
- Header: `X-Tenant-Id` (default)
- Or JWT claim: `tenant_id`

Supported modes via `app.tenancy.mode` (`COLUMN | SCHEMA | DATABASE`):
- `COLUMN`: uses a `tenant_id` column + tenant-scoped queries (default).
- `SCHEMA`: switches DB schema per tenant (requires tenant schemas to exist).
- `DATABASE`: routes to tenant-specific datasources (configure tenant map).

## Security

The API is protected via Spring Security OAuth2 Resource Server (JWT). For local/dev, HS256 is enabled by default via `app.security.jwt.hs256.*`.

Important: this project is a **resource server** (it validates JWTs). It does not provide a “login” endpoint. For local testing, mint a dev HS256 JWT (see `TESTME.md`) or configure a real issuer/JWKS.

Example request (provide a valid JWT as `TOKEN`):

- `curl -H 'Authorization: Bearer TOKEN' -H 'X-Tenant-Id: tenant-a' http://localhost:8080/api/tenant/current`

Swagger UI:
- `http://localhost:8080/swagger-ui.html`

## Tests

- `./mvnw test`

## Actuator

Actuator is enabled. Public endpoints:
- `GET /actuator/health`
- `GET /actuator/info`

Other actuator endpoints (example: `/actuator/metrics`) require JWT.
