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

## 4) Create a local dev JWT (HS256) and call authenticated endpoints

The compose setup configures HS256 locally via:
- `APP_SECURITY_JWT_HS256_ENABLED=true`
- `APP_SECURITY_JWT_HS256_SECRET=change-me-change-me-change-me-32bytes!!`

Use this **Python (stdlib-only)** snippet to mint a token:

- `python3 - <<'PY'
import base64, hashlib, hmac, json, time

def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")

secret = b"change-me-change-me-change-me-32bytes!!"
now = int(time.time())

header = {"alg": "HS256", "typ": "JWT"}
claims = {
    "sub": "test-user",
    "iat": now,
    "exp": now + 3600,
    # Roles are mapped to authorities as ROLE_* by the app
    "roles": ["ADMIN"],
}

msg = f"{b64url(json.dumps(header,separators=(',',':')).encode())}.{b64url(json.dumps(claims,separators=(',',':')).encode())}".encode()
sig = hmac.new(secret, msg, hashlib.sha256).digest()
print(msg.decode() + "." + b64url(sig))
PY`

Export it (replace the output):
- `export TOKEN="<PASTE_TOKEN_HERE>"`

### 4.1 Verify tenant resolution

Header wins over claim, so just pass `X-Tenant-Id`:

- `curl -sS -H "Authorization: Bearer $TOKEN" -H "X-Tenant-Id: tenant-a" http://localhost:8080/api/tenant/current | jq .`

Expect: `tenantId` is `tenant-a`.

## 5) Validate tenant isolation (COLUMN mode)

### 5.1 Create a user in tenant-a

- `curl -sS -H "Authorization: Bearer $TOKEN" -H "X-Tenant-Id: tenant-a" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","displayName":"Alice"}' \
  http://localhost:8080/api/users | jq .`

### 5.2 Query in tenant-a (should be found)

- `curl -i -H "Authorization: Bearer $TOKEN" -H "X-Tenant-Id: tenant-a" \
  "http://localhost:8080/api/users?email=alice@example.com"`

Expect: `200 OK`.

### 5.3 Query in tenant-b (should NOT be found)

- `curl -i -H "Authorization: Bearer $TOKEN" -H "X-Tenant-Id: tenant-b" \
  "http://localhost:8080/api/users?email=alice@example.com"`

Expect: `404 Not Found`.

### 5.4 Create same email in tenant-b (should succeed)

- `curl -i -H "Authorization: Bearer $TOKEN" -H "X-Tenant-Id: tenant-b" \
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

- **401 on everything**: expected; only Swagger is public. Use the HS256 token steps.
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
