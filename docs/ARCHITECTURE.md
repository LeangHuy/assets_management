# Architecture

## System purpose

Asset management service: register and manage devices, and enforce an active Ed25519-signed license (device-only `limits.devices_limit` + local server fingerprint binding) on the server where the product runs. Consumed by `assets-management-ui`. Licenses are issued by `license-key-format`; this service never signs licenses.

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
| `device` | Device CRUD and inventory |
| `license` | `.lic` upload/activate/status; server fingerprint binding; device-only `devices_limit` claim allowlist |
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
3. Service verifies Ed25519 signature (public key), validates payload, and requires exactly `limits.devices_limit` with empty `features` (rejects OTORAS extras such as `database_limit` / `i_service`). Upload path rejects non-`.lic` files and non-`HNS.` wire keys via `LicenseFileReader`.
4. Service computes a host-only server fingerprint, stores the `.lic` and a `fingerprint.meta` sidecar beside `license.storage.path`.
5. Operators may generate offline LMS request files: TEMPORARY → `official-license-request.json` (`CONVERSION`); OFFICIAL (ACTIVE/EXPIRED) → `renewal-official-license-request.json` (`RENEWAL`).
6. Status and device-create paths re-verify the file and recompute the fingerprint; mismatch yields `BINDING_INVALID`.
7. Device create is blocked when license is missing, invalid, expired, or binding-invalid, or when device count reaches `devices_limit`.

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
- Persist `license.storage.path` (and its `fingerprint.meta` sidecar) on a host-mounted volume so container recreation keeps local TEMPORARY binding cache.
- Full Docker stack lives in sibling folder `assets-management-docker/` (MariaDB + backend + UI + nginx).
- Public entry: HTTPS on host port **9005** (`https://<PUBLIC_HOST>:9005`); nginx terminates TLS and proxies `/` → UI, `/api/` → backend.
- App `Dockerfile` remains in this repo for image builds; compose and ops config are in `assets-management-docker`.

## Important technical constraints

- Public key only — private signing key stays in `license-key-format`.
- Exactly one active license file per installation (singleton replace-on-activate).
- Server fingerprint: host-only Phase A binding for TEMPORARY (`fingerprintVersion` 2); OFFICIAL licenses carry issuer-embedded `serverFingerprint` enforced on activate/status.
- Keep API DTOs separate from persistence models.
- Annotation-based MyBatis mappers (no XML).
- Prefer additive SQL under `src/main/resources/schema/`.
- Do not commit secrets or real `.env` credentials.
