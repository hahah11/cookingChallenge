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

`backend/compose.yaml` stays the local-dev stack that `spring-boot-docker-compose` starts
automatically — Postgres, plus a Mailpit SMTP sink added with the email feature (see
`email-notifications-plan.md`).

## Two constraints that shaped the Dockerfiles

1. **Both build contexts are the repository root.** `backend/build.gradle.kts` resolves the
   spec at `../openapi/cookingchallenge-api.yaml`, and the frontend regenerates its client from
   the same file. Neither image can be built from its own subdirectory.
2. **The frontend build stage needs a JRE.** `frontend/.gitignore` excludes
   `src/app/core/api/generated`, so `prebuild` → `generate:api` must run inside the image, and
   `@openapitools/openapi-generator-cli` shells out to `java`. Hence
   `apk add openjdk21-jre-headless` in the Node stage.

## Configuration

Spring's relaxed environment binding maps the compose environment onto the existing
`application.yaml` — no profiles, and no per-environment config files:

| Env var | Overrides |
|---------|-----------|
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | `spring.datasource.*` |
| `APP_FRONTEND_BASE_URL` | `app.frontend.base-url` |
| `APP_VERSION` | `app.version` — set by the image itself, not `.env` (see Image versioning) |
| `APP_MAIL_ENABLED` / `APP_MAIL_FROM` | `app.mail.*` (see `email-notifications-plan.md`) |
| `SPRING_MAIL_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | `spring.mail.*` |

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

GHCR, free. `GITHUB_TOKEN` authenticates the push from Actions. The packages are **public**
(verified 2026-09-22: an anonymous registry token lists their tags), so storage is unmetered and
the server needs no credentials to pull — the `docker login` in Server setup below is only
required if they are ever made private.

The `prune` job deletes *untagged* manifests, keeping the last five. That is housekeeping, not a
quota measure: the ~500 MB cap GitHub applies to private package storage does not apply here.
Version tags are never pruned.

## Image versioning

Every push to `main` publishes three tags per image:

| Tag | Moves? | Use |
|-----|--------|-----|
| `0.1.<run>` | Never | The version of record. Shown on the login page and in `GET /api/v1/config`. |
| `latest` | Every push | What `compose.prod.yaml` runs by default. |
| `sha-<commit>` | Never | Traceability back to an exact commit. |

`0.1` is `BASE_VERSION` in `.github/workflows/publish-images.yml`; the patch component is the
workflow run number. Bump `BASE_VERSION` by hand to start a new line — run numbers keep counting
through a bump, so a version is never reused. Note run numbers are per workflow *file* and would
restart at 1 if that file were renamed.

The number is not semantic: `0.1.42 → 0.1.43` says nothing about what changed. It is a build
identity, chosen so that releasing costs nothing beyond pushing.

The workflow passes it as `--build-arg APP_VERSION`, which `docker/backend.Dockerfile` turns into
an `APP_VERSION` env var. Spring's relaxed binding reads that as `app.version`, and `ConfigService`
puts it on `GET /api/v1/config`, where the Angular login page displays it. A backend built outside
the workflow reports `dev`. The frontend image bakes in no version of its own — it shows the
backend's, so there is only one source of truth.

After both images are pushed, the workflow's `release` job records the version in git as well: it
writes it to `VERSION` at the repo root, commits `Release 0.1.<run> [skip ci]` to `main`, and tags
that commit with an annotated tag named exactly like the image tag (`0.1.<run>`). The tag message
names the commit the images were built from; the tagged commit differs from it only by `VERSION`.
Pushes made with `GITHUB_TOKEN` don't trigger workflows, so the commit causes no rebuild. Because
CI commits to `main` after every push, pull before committing locally. `VERSION` is a record only —
nothing reads it at build or run time.

**Rollback:** set `IMAGE_TAG=0.1.<previous>` in `.env` and `up -d`. Confirm on the login page.

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
docker compose -f compose.prod.yaml up -d
```

`up -d` is the whole deploy. Both app services set `pull_policy: always`, so compose checks the
registry on every start and downloads only when the digest actually changed — an unchanged image
costs one HTTP request.

This matters because `up` on its own pulls *only* when no local image carries the requested tag.
Without `pull_policy`, a moved `latest` stayed stale forever and the stack came up healthy on the
old build with no indication anything had been skipped — which is exactly what happened deploying
the email feature on 2026-09-22. The separate `docker compose pull` step this runbook used to
carry is no longer needed.

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
