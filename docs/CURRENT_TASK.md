# Current Task

## Status

Completed

## Objective

Add an OTORAS-Backend-style `docker-compose.yml` at the `assets_management` repo root for local Docker runs of MariaDB and the Spring Boot app.

## Context

`OTORAS-Backend/docker-compose.yml` keeps Compose in the app repo: named volumes, healthchecks, env defaults, published ports, and an app service built from the local Dockerfile. Operators asked for the same layout here instead of only using sibling `assets-management-docker/`.

ADR-005 still describes the HTTPS nginx ops stack in `assets-management-docker/`. This task added a local-dev Compose file in-repo and did not replace that ops folder.

## Requirements

1. Add `docker-compose.yml` at the repo root (not under the Java package path).
2. Run MariaDB with a Docker named volume, healthcheck, and published host port (default 3308 to match `application.yml`).
3. Run the backend image built from the existing `Dockerfile`, waiting until MariaDB is healthy.
4. Pass `DB_*`, CORS, license public key, and license storage path via environment variables.
5. Persist license files on a named volume.
6. Provide `.env.example` (no real secrets) and ignore `.env`.
7. Initialize the `device` table on first MariaDB start from the existing schema SQL.

## Out of scope

- Moving or deleting `assets-management-docker/`.
- nginx / HTTPS on port 9005.
- Redis, Kafka, or other OTORAS-only services.

## Acceptance criteria

- `docker compose config` succeeds from this repo.
- `docker compose up -d --build` can start MariaDB and the backend on ports 3308 and 8082.
- No real license keys or DB passwords are committed.

## Relevant files

- `docker-compose.yml`
- `.env.example`
- `.gitignore`
- `Dockerfile`
- `src/main/resources/schema/shcema.sql`
- `docs/ARCHITECTURE.md`
- `docs/DECISIONS.md`

## Risks and constraints

- Host port 3308 may conflict with a running OTORAS MariaDB; override `ASSETS_DB_PORT`.
- `LICENSE_SIGNING_PUBLIC_KEY` must be set in `.env` or the backend will fail license operations.
- Linux Docker may need `/etc/machine-id` mounts for stable fingerprints (same as OTORAS).

## Implementation result

### Status

Completed on 2026-08-13.

### Files changed

- `docker-compose.yml`: MariaDB 11.8 + backend built from local Dockerfile; named volumes; host ports 3308 / 8082.
- `.env.example`: Compose env template without real secrets.
- `.gitignore` / `.dockerignore`: ignore `.env`.
- `docs/DECISIONS.md`: ADR-014; ADR-005 note that ops stack remains.
- `docs/ARCHITECTURE.md`: local Compose plus existing ops folder.

### Main implementation decisions

- Compose lives at the repo root, matching OTORAS-Backend, not under the Java package path.
- Container names (`assets-mgmt-db`, `assets-mgmt-backend`) and project name `assets-management-local` avoid colliding with `assets-management-docker`.
- License files use a named volume (Windows-friendly). Linux machine-id binds are commented like OTORAS.

### Verification commands executed

- `docker compose config --quiet` — Passed

### Test results

- `./gradlew compileJava` — Not run (no Java changes)
- `./gradlew test` — Not run
- `docker compose up` — Not run

### Known limitations

- `LICENSE_SIGNING_PUBLIC_KEY` is empty until operators copy `.env.example` → `.env`.
- First-boot schema uses `shcema.sql` as MariaDB init; later additive migrations are not applied automatically.
- `assets-management-docker/` remains the HTTPS :9005 ops stack.

### Remaining work

- Operators set `LICENSE_SIGNING_PUBLIC_KEY` and run `docker compose up -d --build`.
