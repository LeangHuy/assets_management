# Architecture

## System purpose

Asset management service: register and manage devices, and enforce an active Ed25519-signed license (`limits.devices_limit`, `features.import_feature` + local server fingerprint binding) on the server where the product runs. Consumed by `assets-management-ui`. Licenses are issued by `license-key-format`; this service never signs licenses.

## Technology stack

- Java 21 / Spring Boot 4 (Web MVC)
- MyBatis + MariaDB (`assets_db`)
- springdoc OpenAPI UI
- Jakarta Bean Validation
- Lombok
- `hns-license-lib` for signature verification, on-disk `.lic` storage, and shared host fingerprint compute

## Major modules

| Package | Responsibility |
| --- | --- |
| `device` | Device CRUD, CSV-driven bulk import (create-or-skip), inventory |
| `license` | `.lic` upload/activate/status; server fingerprint binding; `devices_limit` + `import_feature` claim allowlist |
| `common` | API envelope, CORS, OpenAPI, exceptions |

## Directory structure

```
src/main/java/com/hunesion/assets_management/
  device/      controller, service, repository, dto, domain
  license/     controller, service, dto, fingerprint binding/request stores
  common/      shared config and exceptions
src/main/resources/
  application.yml
  schema/      SQL baseline + migrations
docs/
libs/          hns-license-lib JAR
```

## Data flow

1. Admin UI calls REST APIs on port `8082`.
2. Operator uploads a signed `.lic` from `license-key-format`.
3. Service verifies Ed25519 signature (public key), validates payload, and requires exactly `limits.devices_limit` and `features.import_feature` (rejects OTORAS extras such as `database_limit` / `i_service`). Upload path rejects non-`.lic` files and non-`HNS.` wire keys via `LicenseFileReader`.
4. Service computes a host-only server fingerprint, stores the `.lic` and a `fingerprint.meta` sidecar beside `license.storage.path`.
5. Operators may generate offline LMS request files: TEMPORARY → `official-license-request.json` (`CONVERSION`); OFFICIAL (ACTIVE/EXPIRED) → `renewal-official-license-request.json` (`RENEWAL`).
6. Status and device-create paths re-verify the file and recompute the fingerprint; mismatch yields `BINDING_INVALID`.
7. Device create and import are blocked when license is missing, invalid, expired, or binding-invalid, or when device count reaches `devices_limit`.
8. `POST /api/v1/devices/import` requires `features.import_feature = true`, create-or-skips by ACTIVE name (case-insensitive), and each new insert reuses `assertCanCreateDevice()`. CSV parsing stays in `assets-management-ui`.

## Authentication and authorization

- TODO: Confirm production auth integration (sibling issuer uses i-oneSSO JWT).

## External integrations

- MariaDB via `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`.
- CORS origins for admin UIs (`CORS_ALLOWED_ORIGINS`).
- License public key via `LICENSE_SIGNING_PUBLIC_KEY` / `license.signing.public-key`.
- Active license path via `LICENSE_STORAGE_PATH` / `license.storage.path` (default `data/license/active.lic`).
- Admin UI: `assets-management-ui`.
- Issuer: `license-key-format` (issuance only; Phase A has no online activation registry).

## Deployment model

- Runnable Spring Boot JAR (`./gradlew bootRun` / packaged jar).
- Default server port: `8082`.
- Persist `license.storage.path` (and its `fingerprint.meta` sidecar) on a Docker named volume (or host bind) so container recreation keeps local TEMPORARY binding cache.
- Local Docker: `docker-compose.yml` in this repo (MariaDB + backend), OTORAS-Backend style. Copy `.env.example` → `.env`.
- Full HTTPS ops stack remains in sibling folder `assets-management-docker/` (MariaDB + backend + UI + nginx).
- Public ops entry: HTTPS on host port **9005** (`https://<PUBLIC_HOST>:9005`); nginx terminates TLS and proxies `/` → UI, `/api/` → backend.
- App `Dockerfile` in this repo is used by both local Compose and `assets-management-docker`.

## Important technical constraints

- Public key only — private signing key stays in `license-key-format`.
- Exactly one active license file per installation (singleton replace-on-activate).
- Server fingerprint: host-only Phase A binding for TEMPORARY (`fingerprintVersion` 2); OFFICIAL licenses carry issuer-embedded `serverFingerprint` enforced on activate/status.
- Keep API DTOs separate from persistence models.
- Annotation-based MyBatis mappers (no XML).
- Prefer additive SQL under `src/main/resources/schema/`.
- Do not commit secrets or real `.env` credentials.
