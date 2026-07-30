# Current Task

## Status

Completed

## Objective

Consolidate the full Docker stack into a single sibling folder `assets-management-docker` that includes compose, nginx, certs, env, and Dockerfiles for UI + backend.

## Context

Ops files were previously mixed into `assets_management`. Operator wants one folder that contains everything needed to run HTTPS on port 9005.

## Requirements

1. Create `assets-management-docker/` next to both app repos.
2. Move compose, nginx, MariaDB init, certs, env template into that folder.
3. Include backend/ui Dockerfiles in that folder; build contexts stay on sibling source repos.
4. Remove compose/nginx/certs clutter from `assets_management`.
5. Document run steps in the new folder README.

## Out of scope

- Filling production secrets in `.env.docker`
- CA-signed certificates

## Acceptance criteria

- Operator can `cd assets-management-docker && docker compose --env-file .env.docker up -d --build`
- Sibling `assets_management` and `assets-management-ui` are used as build contexts

## Implementation result

### Status

Completed on 2026-07-30.

### Files changed

- Created `../assets-management-docker/` with full stack
- Removed `docker-compose.yml`, `docker/`, `.env.docker*` from this repo
- Updated `docs/ARCHITECTURE.md` and ADR-005 to point at the deploy folder
- App `Dockerfile` / `.dockerignore` kept here for source-context builds

### Verification

- `docker compose ... config --services` from new folder — Passed (`db`, `ui`, `backend`, `nginx`)

### Known limitations

- `.env.docker` must still set `LICENSE_SIGNING_PUBLIC_KEY` before backend stays healthy

### Remaining work

- Operator configures `.env.docker` and starts the stack from the new folder
