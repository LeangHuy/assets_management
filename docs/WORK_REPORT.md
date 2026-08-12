# Work Reports

## 2026-08-12 (IMPORT_FEATURE + DEVICES_LIMIT claim alignment)

### Today's Work

- Updated license activation to accept `limits.devices_limit` and `features.import_feature` only.
- Added `assertCanImportDevices()` so bulk import requires `features.import_feature = true`.
- Wired device import to call the new import feature gate before processing rows.

### Technical Changes

- `LicenseServiceImpl.java`: claim allowlist + `assertCanImportDevices()`.
- `LicenseService.java`: new assert API.
- `DeviceServiceImpl.java`: import calls `assertCanImportDevices()` first.
- `docs/DECISIONS.md`: ADR-013; ADR-011 superseded.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run
- Build: Passed (`./gradlew compileJava --rerun-tasks`)

### Known Issues

- UI still shows Import when `import_feature` is false (API returns 403).

### Next Work Plan

- Optionally hide Import in `assets-management-ui` based on license `features`.

## 2026-08-12 (device import + license limit)

### Today's Work

- Added bulk device import that create-or-skips by active name.
- Each new import row is gated by licensed `limits.devices_limit` (same rule as single create).
- Documented the import contract as ADR-012.

### Technical Changes

- `DeviceController.java`: `POST /api/v1/devices/import`.
- `DeviceServiceImpl.java`: create-or-skip import with `assertCanCreateDevice()` per insert.
- `DeviceRepository.java`: `findActiveByNameIgnoreCase`.
- `DeviceImport*.java`: request/result/error DTOs.
- `docs/DECISIONS.md` / `docs/ARCHITECTURE.md`: ADR-012 and data-flow notes.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run
- Build: Passed (`./gradlew compileJava --rerun-tasks`)

### Known Issues

- Mid-batch limit failures leave earlier successful creates committed.
- Skip matches ACTIVE names only; INACTIVE same-name rows do not block import.

### Next Work Plan

- Optional unique name constraint / broader skip rules.
- Manual import test against a license with a small `devices_limit`.

## 2026-08-11 (device-only claim allowlist)

### Today's Work

- Corrected assets_management activation so foreign-product claims are rejected.
- A license with `devices_limit` plus `i_service` / `database_limit` / any other features can no longer activate on this service.
- Kept safer activate hygiene from the earlier OTORAS alignment pass.

### Technical Changes

- `LicenseServiceImpl.java`: `requireAssetManagementClaims()` requires exactly `limits.devices_limit` and empty `features`.
- `LicenseService.java`: removed unused database / I-Service assert APIs.
- `docs/DECISIONS.md`: ADR-011 accepted; ADR-010 superseded.
- `docs/ARCHITECTURE.md` / `AGENTS.md`: documented device-only claim contract.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run
- Build: Passed (`./gradlew compileJava --rerun-tasks`)

### Known Issues

- Claim allowlist is enforced on activate, not re-checked on every status call for an already-written file.
- Product code (`payload.product`) is not yet part of the reject rule.

### Next Work Plan

- Optionally enforce `payload.product` for assets_management.
- Issue assets_management licenses with only `devices_limit` and empty features from LMS.

## 2026-08-11 (OTORAS-aligned license claim gates)

### Today's Work

- Expanded assets_management license activation to require `devices_limit`, `database_limit`, and `features.i_service`.
- Added runtime assert APIs for database equipment and I-Service while keeping device create on the existing gate.
- Hardened activate hygiene by removing payload/fingerprint INFO logs and dropping unnecessary `@Transactional` on read-only license paths.

### Technical Changes

- `LicenseServiceImpl.java`: activation claim checks, `features` in status mapping, new asserts, no sensitive activate logging, no read-only `@Transactional`.
- `LicenseService.java` / `LicenseInfoResponse.java`: new API surface and `features` field.
- `docs/DECISIONS.md`: ADR-010; ADR-002 updated.
- `docs/ARCHITECTURE.md` / `AGENTS.md`: documentation aligned with the new claim contract and `hns-license-lib` 1.0.4.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run (no test sources)
- Build: Passed (`./gradlew compileJava test --rerun-tasks`)

### Known Issues

- Database-limit and I-Service asserts are not wired to any domain call sites yet.
- Licenses without the new claims cannot be re-activated until re-issued.

### Next Work Plan

- Wire the new asserts when DB-equipment or I-Service flows are added.
- Optionally update `assets-management-ui` license types to display `features` and `database_limit`.

## 2026-08-05 (consume versioned ServerFingerprint)

### Today's Work

- Updated license activate/resolve to use versioned `ServerFingerprint` from `hns-license-lib` 1.0.3.

### Technical Changes

- `LicenseServiceImpl.java`: persists `fingerprint.value()` and `fingerprint.version()` into `fingerprint.meta`.
- `build.gradle` / `AGENTS.md`: depend on `hns-license-lib-1.0.3.jar`.
- ADR-001 / ADR-009 updated.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run
- Build: Passed (`./gradlew compileJava --rerun-tasks`)

### Known Issues

- Unused older JARs may remain under `libs/`.

### Next Work Plan

- Remove leftover older JARs when not locked.

## 2026-08-05 (consume lib fingerprint compute)

### Today's Work

- Switched server fingerprint computation to `hns-license-lib` 1.0.2.
- Removed the local `ServerFingerprintProvider` class.

### Technical Changes

- `build.gradle` / `AGENTS.md`: depend on `libs/hns-license-lib-1.0.2.jar`.
- `LicenseServiceImpl.java`: injects `com.hunesion.license.runtime.fingerprint.ServerFingerprintProvider`.
- Deleted local `license/fingerprint/ServerFingerprintProvider.java`.
- ADR-009; ADR-001 / ARCHITECTURE updated.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run
- Build: Passed (`./gradlew compileJava --rerun-tasks`)

### Known Issues

- `libs/hns-license-lib-1.0.1.jar` could not be deleted (file locked); unused by build.gradle.

### Next Work Plan

- Delete the leftover 1.0.1 JAR when the process lock is released.

## 2026-08-05 (host-only fingerprint, no installationId)

### Today's Work

- Removed `installation_id` from the server fingerprint formula so binding is host-only.
- Deleted local `installation.meta` identity store usage.
- Stopped emitting `installationId` in offline official-license request JSON.

### Technical Changes

- `ServerFingerprintProvider.java`: `SHA-256(os_machine_id|host_identity|primary_mac)`; `FINGERPRINT_VERSION = 2`.
- Deleted `InstallationMeta.java` and `ServerIdentityStore.java`.
- `OfficialLicenseRequest.java` / `OfficialLicenseRequestStore.java` / `LicenseServiceImpl.java`: no installation id.
- ADR-008 accepted; ADR-003 superseded; architecture docs updated.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run
- Build: Passed (`./gradlew compileJava`)

### Known Issues

- Existing OFFICIAL licenses signed with v1 fingerprints fail activate until re-requested and re-issued.
- Orphan `installation.meta` files are left on disk but unused.

### Next Work Plan

- Redeploy with matching issuer/UI changes and re-issue OFFICIAL licenses under fingerprint version 2.
- Consider moving fingerprint compute into `hns-license-lib` for other products.

## 2026-08-04 (renewal request filename)

### Today's Work

- Named renewal downloads `renewal-official-license-request.json` (conversion keeps `official-license-request.json`).

### Technical Changes

- `OfficialLicenseRequestStore`: writes the filename based on `requestType`.
- `LicenseService.LicenseRequestFile` + controller `Content-Disposition` use that name.

### Verification

- Build: Passed (`./gradlew compileJava`)

### Known Issues

- None.

### Next Work Plan

- Redeploy SRA and verify the downloaded renewal filename in the browser.

## 2026-08-04 (renewal request generation)

### Today's Work

- Allowed bound OFFICIAL licenses (ACTIVE or EXPIRED) to generate offline renewal requests.
- Kept TEMPORARY conversion request generation for first official issue.

### Technical Changes

- `OfficialLicenseRequest.java`: added `requestType` (`CONVERSION` | `RENEWAL`).
- `LicenseServiceImpl.generateOfficialLicenseRequest`: branches by license type and status.
- `docs/DECISIONS.md`: ADR-007; `docs/ARCHITECTURE.md` data-flow step for request generation.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run
- Build: Passed (`./gradlew compileJava`)

### Known Issues

- Requires issuer ADR-016 deployed to accept RENEWAL files.

### Next Work Plan

- Redeploy with issuer/UI and verify renewal E2E.

## 2026-07-31

### Today's Work

- Enforced issuer-embedded `serverFingerprint` so an OFFICIAL `.lic` cannot activate on a different server.
- Refreshed `hns-license-lib` with the optional payload field.

### Technical Changes

- `LicenseServiceImpl.java`: Rejects activate with 403 on payload fingerprint mismatch; resolve returns `BINDING_INVALID` when payload or local meta mismatches.
- `libs/hns-license-lib-1.0.1.jar`: Refreshed from `license-runtime`.
- `docs/DECISIONS.md`: Superseded ADR-004; added ADR-006.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run
- Build: Passed (`./gradlew compileJava`)

### Known Issues

- Already-issued OFFICIAL licenses without embedded fingerprint still activate on any server until re-issued.
- Docker hosts must rebuild/redeploy with the new JAR and a newly issued OFFICIAL license to verify cross-server rejection.

### Next Work Plan

- Redeploy assets_management on both servers, re-request/re-issue OFFICIAL from server 1, confirm server 2 activate fails.

## 2026-07-30 (activation-request JSON alignment)

### Today's Work

- Aligned `official-license-request.json` export with the issuer activation-request schema.
- Stopped embedding customer/product fields that the issuer must resolve from the demo license.

### Technical Changes

- `OfficialLicenseRequest.java`: Nested `formatVersion`, `requestId`, `demoLicense`, `installation` (with `fingerprintComputedAt`).
- `LicenseServiceImpl.java`: Builds the new shape and generates a UUID `requestId` per export.
- `OfficialLicenseRequestStore.java`: Updated write logging for nested fields.
- `docs/DECISIONS.md`: Updated ADR-004.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Passed (`./gradlew test`, no test sources)
- Build: Passed (`./gradlew compileJava`)

### Known Issues

- Existing flat request JSON files must be regenerated.
- Old `fingerprint.meta` with `boundAt` still needs re-activation for `computedAt`.

### Next Work Plan

- Re-generate and upload a request in license-key-ui to confirm end-to-end preview/submit.

## 2026-07-30

### Today's Work

- Explained and closed the gap where `official-license-request.json` was never generated in Phase A.
- Renamed fingerprint meta timestamp from `boundAt` to `computedAt`.
- Added offline official license request generation that downloads and stores `official-license-request.json`.

### Technical Changes

- `FingerprintMeta.java`: Renamed `boundAt` to `computedAt`.
- `OfficialLicenseRequest.java` / `OfficialLicenseRequestStore.java`: Offline request file model and persistence.
- `LicenseServiceImpl.java` / `LicenseController.java`: `POST /api/v1/license/official-request` builds the file from TEMPORARY active bound license.
- `docs/DECISIONS.md`: Updated ADR-004 for request export.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Passed (`./gradlew test`, no test sources)
- Build: Passed (`./gradlew compileJava`)

### Known Issues

- Issuer-side upload/approve of the JSON and official `.lic` issuance are not implemented yet.
- Old `fingerprint.meta` with `boundAt` should be regenerated by re-activating the demo license.

### Next Work Plan

- Accept `official-license-request.json` in `license-key-format` and issue `official-license.lic`.

## 2026-07-30

### Today's Work

- Consolidated the assets management Docker stack into a new sibling folder `assets-management-docker`.
- Moved compose, nginx, MariaDB init, certs, env template, and Dockerfiles into that single deploy folder.
- Removed compose/nginx/certs files from the `assets_management` application repo.

### Technical Changes

- `../assets-management-docker/docker-compose.yml`: Full stack; builds from sibling `assets_management` and `assets-management-ui`.
- `../assets-management-docker/backend/Dockerfile` and `ui/Dockerfile`: Image build definitions for the stack.
- `../assets-management-docker/nginx/`: nginx:latest with TLS auto-cert entrypoint.
- `docs/ARCHITECTURE.md` / `docs/DECISIONS.md`: Point deployment to `assets-management-docker`.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run
- Build: Compose config validated (`db`, `ui`, `backend`, `nginx`)

### Known Issues

- Backend still requires `LICENSE_SIGNING_PUBLIC_KEY` in `.env.docker` before it stays healthy.

### Next Work Plan

- Set `.env.docker` secrets and run `docker compose --env-file .env.docker up -d --build` from `assets-management-docker`.

## 2026-07-30

### Today's Work

- Added Docker packaging for assets management: backend image, UI image, MariaDB, and nginx HTTPS on port 9005 in one compose stack.
- Configured nginx to terminate TLS and reverse-proxy the Next.js UI and `/api` to the Spring Boot backend.
- Documented env setup (`.env.docker.example`) and self-signed cert generation for IP-based HTTPS.

### Technical Changes

- `Dockerfile`: Multi-stage Java 21 build producing a runnable Spring Boot image.
- `docker-compose.yml`: Runs MariaDB, backend, UI, and nginx; publishes host port 9005 → 443.
- `docker/nginx/*`: nginx:latest with OpenSSL entrypoint that auto-creates TLS certs for `PUBLIC_HOST`.
- `docker/init-db/01-schema.sql`: Creates the `device` table on first MariaDB start.
- `.env.docker.example`: Template for `PUBLIC_HOST`, DB password, CORS, and license public key.
- `docs/DECISIONS.md`: Added ADR-005 for the compose + nginx HTTPS deployment.
- Sibling `assets-management-ui`: Dockerfile + `output: "standalone"` for production container.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Not run
- Build: Passed (`docker compose --env-file .env.docker build`)
- Runtime: UI reachable on `https://127.0.0.1:9005` (307); backend restarting until `LICENSE_SIGNING_PUBLIC_KEY` is set in `.env.docker`

### Known Issues

- Backend requires a non-empty `LICENSE_SIGNING_PUBLIC_KEY` in `.env.docker` or it crash-loops.
- Self-signed certificates show browser security warnings.

### Next Work Plan

- Set production `PUBLIC_HOST`, CORS origins, and license public key, then recreate the backend container.
- Optionally replace `docker/certs` with CA-signed certificates.


## 2026-07-30

### Today's Work

- Created the missing `assets_management` project documentation (`AGENTS.md`, architecture, decisions).
- Implemented Phase A local server-fingerprint binding with file sidecars beside the active `.lic`.
- On activate, the service now binds a computed fingerprint and returns `serverBound=true`.
- Status and device-create paths now detect fingerprint mismatch as `BINDING_INVALID` and block new devices.
- Refreshed `hns-license-lib` with the shared `BINDING_INVALID` status constant.

### Technical Changes

- `AGENTS.md`: Added agent workflow and stack instructions.
- `docs/ARCHITECTURE.md`: Documented device + local license enforcement architecture.
- `docs/DECISIONS.md`: Added ADR-001–004 including local fingerprint binding and offline-only future issuer transport.
- `license/fingerprint/ServerFingerprintProvider.java`: Computes SHA-256 server fingerprint from installation and host attributes.
- `license/fingerprint/ServerIdentityStore.java`: Persists `installation.meta`.
- `license/fingerprint/LicenseBindingStore.java`: Persists `fingerprint.meta`.
- `LicenseServiceImpl.java`: Binds fingerprint on activate and validates it on status and device create.
- `libs/hns-license-lib-1.0.1.jar`: Updated from `license-runtime` with `BINDING_INVALID`.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Passed (`./gradlew test`, no test sources)
- Build: Passed (`./gradlew compileJava`)

### Known Issues

- Phase A is local-only; the same `.lic` can still be activated on multiple servers until offline issuer binding exists.
- End-to-end fingerprint mismatch was not verified on a live running instance in this session.

### Next Work Plan

- Add offline activation request/certificate file exchange with `license-key-format`.
- Add automated tests for activate / binding mismatch / device block paths.


## 2026-07-24

### Today's Work

- Integrated `license-runtime` JAR and removed duplicated local license verification/storage code from `assets_management`.
- Kept product-specific device-limit enforcement in `assertCanCreateDevice()` with `limits.devices` required on activation.
- Added global handling for library `LicenseException` responses.

### Technical Changes

- `build.gradle`: Added Maven Local repository and `license-runtime` dependency.
- `LicenseServiceImpl.java`: Refactored to use `com.hunesion.license.runtime.*`; activation requires `limits.devices`.
- `GlobalExceptionHandler.java`: Maps `LicenseException` to `ApiResponse` failures.
- `TimeConfig.java`: Clock bean is `@ConditionalOnMissingBean`.
- Removed 13 duplicate classes from `license/crypto`, `license/signing`, `license/storage`, `license/support`, and `license/domain`.

### Verification

- Type check: Not run
- Lint: Not run
- Tests: Passed (`./gradlew test`)
- Build: Passed (`./gradlew compileJava`)

### Known Issues

- None.

### Next Work Plan

- None for license-runtime integration.
