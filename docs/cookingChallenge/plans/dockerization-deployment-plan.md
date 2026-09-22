# Dockerization & Deployment Plan

Run the whole stack as three containers on a spare x86_64 notebook acting as the LAN server,
with images built by GitHub Actions and pulled from GitHub Container Registry (GHCR).

## Topology

```
LAN ──:80──▶ frontend (nginx:1.29-alpine)          published
                 ├── /            → Angular build (dist/frontend/browser)
                 └── /api/        → proxy_pass http://backend:8080
             backend  (eclipse-temurin:25-jre)     internal only
                 └── jdbc://db:5432/cookoff
             db       (postgres:17-alpine)         internal only, named volume db-data
```

Only the frontend publishes a port. nginx is the single origin, which is what keeps
`frontend/src/app/core/api/api-config.ts` on `provideApi('')`: no CORS configuration, and no
API base URL baked into the bundle at build time.

## Files

| File | Purpose |
|------|---------|
| `docker/backend.Dockerfile` | Gradle `bootJar` → JRE runtime, non-root user |
| `docker/frontend.Dockerfile` | `npm ci` + `ng build` → nginx static serve |
| `docker/nginx.conf` | SPA fallback, `/api` proxy, gzip, immutable asset caching |
| `compose.prod.yaml` | The three services, wiring, health gating, volume |
| `.env.example` | Template for the server's `.env` (`.env` is gitignored) |
| `.github/workflows/publish-images.yml` | Build + push to GHCR on every push to `main`, prune untagged |
| `.dockerignore` | Keeps `node_modules`, `build/`, `dist/`, `.git` out of the build context |

`backend/compose.yaml` is unchanged — it stays the local-dev Postgres that
`spring-boot-docker-compose` starts automatically.

## Two constraints that shaped the Dockerfiles

1. **Both build contexts are the repository root.** `backend/build.gradle.kts` resolves the
   spec at `../openapi/cookingchallenge-api.yaml`, and the frontend regenerates its client from
   the same file. Neither image can be built from its own subdirectory.
2. **The frontend build stage needs a JRE.** `frontend/.gitignore` excludes
   `src/app/core/api/generated`, so `prebuild` → `generate:api` must run inside the image, and
   `@openapitools/openapi-generator-cli` shells out to `java`. Hence
   `apk add openjdk21-jre-headless` in the Node stage.

## Configuration

No backend source change is required. Spring's relaxed environment binding maps the compose
environment onto the existing `application.yaml`:

| Env var | Overrides |
|---------|-----------|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | `spring.datasource.*` |
| `APP_FRONTEND_BASE_URL` | `app.frontend.base-url` |

`APP_FRONTEND_BASE_URL` must be the address a guest's phone can reach (the server's LAN IP or
hostname) — it is embedded in access links and their QR codes, so `localhost` breaks them.

Liquibase owns the schema and runs on startup, so the database needs no seeding step.
`JwtConfig` generates an in-memory RSA keypair per start, so there is no signing secret to
manage — but every restart invalidates issued tokens, guests included.

## Health gating

`spring-boot-starter-actuator` exposes `/actuator/health` and nothing else
(`management.endpoints.web.exposure.include: health`, `show-details: never`). The aggregate
status covers the Postgres connection, so a 200 means "ready to serve" and a 503 means "do not
route to me yet" — `curl -f` alone is therefore the whole healthcheck.

`SecurityConfig` permits `GET /actuator/health` anonymously, which it must, since the filter
chain ends in `anyRequest().denyAll()`. The rule is a path matcher rather than
`EndpointRequest.to(HealthEndpoint.class)` to avoid coupling to actuator's autoconfiguration
package, which moved in Boot 4. Anonymous access is safe here: management shares the
application port, that port is never published to the LAN, and `docker/nginx.conf` proxies only
`/api/` — so `/actuator` is reachable only from inside the compose network.
`SecurityIntegrationTest` covers the anonymous 200.

This is what lets `compose.prod.yaml` gate the frontend on `condition: service_healthy` instead
of mere start order — which matters because nginx resolves the `backend` upstream at config
load and exits if it cannot.

## Registry

GHCR, private, free. `GITHUB_TOKEN` authenticates the push from Actions; the server pulls with
a PAT scoped to `read:packages`. GitHub's Free plan caps private package storage at roughly
500 MB, so the workflow's `prune` job deletes untagged versions and keeps the last five.
Images are tagged `latest` and `sha-<commit>`; pinning `IMAGE_TAG` to a SHA in `.env` gives a
one-line rollback.

## Server setup (once)

```bash
echo "$GHCR_PAT" | docker login ghcr.io -u hahah11 --password-stdin
git clone https://github.com/hahah11/cookingChallenge.git && cd cookingChallenge
cp .env.example .env && $EDITOR .env      # password + LAN IP
docker compose -f compose.prod.yaml up -d
```

## Deploy

```bash
git pull                                              # only if compose.prod.yaml changed
docker compose -f compose.prod.yaml pull
docker compose -f compose.prod.yaml up -d
```

## Backup

```bash
docker compose -f compose.prod.yaml exec -T db \
  pg_dump -U cookoff cookoff | gzip > cookoff-$(date +%F).sql.gz
```

## Open follow-ups

- **HTTP only.** Fine on a trusted LAN; JWTs cross the network in clear. If the app is ever
  reachable beyond the LAN, terminate TLS at nginx (Caddy or certbot) before exposing it.
- **Single instance assumed.** Per-instance JWT keys and in-memory state rule out scaling the
  backend past one replica without the changes `JwtConfig` already flags.
