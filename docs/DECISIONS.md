# Architecture Decisions

## ADR-001: Shared license runtime via file JAR

- Status: Accepted
- Context: Ed25519 verify/store logic was duplicated across product services.
- Decision: Depend on `libs/hns-license-lib-1.0.3.jar` (`com.hunesion.license.runtime.*`) for wire decode, signature verify, payload validation, `LicenseFileStore`, and host fingerprint compute. Keep product claim enforcement and binding sidecars in this service.
- Rationale: Matches `account-management` and avoids re-copying crypto/storage code.
- Consequences: After library changes, rebuild/copy the JAR into `libs/` (or switch to `mavenLocal()` coordinates). Configure `LICENSE_SIGNING_PUBLIC_KEY` and `LICENSE_STORAGE_PATH`.
- Date: 2026-07-24

## ADR-002: Device limit from signed `limits.devices_limit`

- Status: Accepted (extended by ADR-011)
- Context: Asset registration must not exceed the licensed device cap.
- Decision: On activate, require `limits.devices_limit`. On create device, count all devices (including recycle-bin statuses) and reject with 409 when at or above the limit.
- Rationale: Claim values live only in the signed payload; the product owns enforcement.
- Consequences: Licenses without `devices_limit` cannot activate on this service.
- Date: 2026-07-24
- Updated: 2026-08-11 — activation claim policy is device-only allowlist (ADR-011); ADR-010 superseded.

## ADR-003: Local server fingerprint binding (Phase A)

- Status: Superseded by ADR-008
- Context: A license must be usable only on the server where it was activated. MAC-only binding is unstable under Docker/VMs.
- Decision: At activate, ensure a persistent `installation_id`, compute `SHA-256(installation_id|os_machine_id|host_identity|primary_mac)`, and store the hash in `fingerprint.meta` beside the `.lic`. On status and device-create, recompute and compare; mismatch returns `BINDING_INVALID` and blocks device creation. Persist identity/binding as file sidecars under the license storage directory (not new DB tables).
- Rationale: Matches the local `/data/.../license/` layout, survives container recreate when the storage path is volume-mounted, and does not require online access to the issuer.
- Consequences: Hardware replacement or lost volume invalidates binding until the operator re-activates the same `.lic`. Issuer-side registry and Activation Certificates remain future work.
- Date: 2026-07-30
- Updated: 2026-08-05 — superseded by ADR-008 (host-only fingerprint, no installation_id).

## ADR-004: Future issuer binding via offline files only

- Status: Superseded by ADR-006
- Context: The full license diagram includes an activation request to the issuer and a signed activation certificate. Network access from customer servers to the issuer may be unavailable.
- Decision: Do not add an online `ActivationRegistryClient` in Phase A. When issuer binding is added later, use offline file export/import only.
- Rationale: Aligns with air-gapped customer deployments and avoids coupling activate to issuer availability.
- Consequences: Phase A cannot prevent the same `.lic` from being activated on two servers that never sync with the issuer; that gap is accepted until offline issuer binding ships.
- Date: 2026-07-30
- Updated: 2026-07-31 — superseded by ADR-006 (fingerprint embedded in signed OFFICIAL payload via offline activation request).

## ADR-005: Compose stack with nginx HTTPS on port 9005

- Status: Accepted
- Context: Operators need a single way to run UI, backend, and MariaDB behind HTTPS on a fixed host port, often by server IP.
- Decision: Keep app Dockerfiles in each repo, and put the full ops stack in sibling folder `assets-management-docker/` (`docker-compose.yml`, nginx, MariaDB init, env template, certs). nginx listens on container 443, published as host **9005**. UI is built with empty `NEXT_PUBLIC_API_BASE_URL` so the browser calls same-origin `/api/...` through nginx. TLS uses self-signed certs auto-generated for `PUBLIC_HOST` (replaceable with CA certs under `certs/`).
- Rationale: One deploy folder matches ops expectations; reverse-proxy avoids exposing backend/UI ports and keeps CORS/TLS at the edge; source repos stay free of compose/certs clutter.
- Consequences: Operators must set `PUBLIC_HOST` / `CORS_ALLOWED_ORIGINS` to the IP or hostname clients use; browsers warn on self-signed certs until a trusted cert is installed; sibling checkouts of `assets_management` and `assets-management-ui` are required next to `assets-management-docker`. Local-dev Compose in the app repos is documented in ADR-014 and does not replace this ops stack.
- Date: 2026-07-30
- Updated: 2026-08-13 — local-dev Compose added in-repo (ADR-014); this ADR remains the HTTPS ops stack.

## ADR-006: Enforce issuer-embedded serverFingerprint for OFFICIAL

- Status: Accepted
- Context: Local `fingerprint.meta` alone rebinds on each activate, so copying an OFFICIAL `.lic` to another server still succeeded. The issuer now embeds `serverFingerprint` in new OFFICIAL payloads (offline activation request → approve).
- Decision: On activate and resolve, if the signed payload has a non-blank `serverFingerprint`, it must match the current host fingerprint; otherwise reject activate with 403 or return `BINDING_INVALID`. TEMPORARY licenses omit the field and keep local bind-on-activate. Already-issued OFFICIAL licenses without the field remain accepted (backward compatible). Keep writing `fingerprint.meta` as a local cache for TEMPORARY request export and host-change detection.
- Rationale: Signed binding closes the cross-server reuse gap without an online registry, matching air-gapped offline file exchange.
- Consequences: Redeploy with refreshed `hns-license-lib`. Operators must re-request/re-issue OFFICIAL to bind existing customers. Local meta alone is no longer sufficient for OFFICIAL enforcement.
- Date: 2026-07-31

## ADR-007: Offline request generation for CONVERSION and RENEWAL

- Status: Accepted
- Context: Official conversion required a TEMPORARY demo. After OFFICIAL activation, engineers could not export another request to update expiry/claims.
- Decision: `POST /api/v1/license/official-request` accepts TEMPORARY (ACTIVE) as `requestType=CONVERSION` and OFFICIAL (ACTIVE or EXPIRED) as `requestType=RENEWAL`, both requiring a valid server binding. Download/persist filenames are `official-license-request.json` (CONVERSION) and `renewal-official-license-request.json` (RENEWAL). The nested `demoLicense` object remains the source-license field for issuer wire compatibility.
- Rationale: Same offline handoff content for first official issue and later renewals; distinct filenames make renewals obvious to operators; issuer distinguishes kinds via `requestType`.
- Consequences: Expired OFFICIAL can still generate renewals; INVALID / BINDING_INVALID cannot. Issuer must understand `RENEWAL` (issuer ADR-016).
- Date: 2026-08-04
- Updated: 2026-08-04 — renewal download filename is `renewal-official-license-request.json`.

## ADR-008: Host-only server fingerprint (no installation_id)

- Status: Accepted
- Context: Including a random `installation_id` in the fingerprint forced operators to re-request OFFICIAL after product data reset on the same host, and required shared meta for multi-product binding on one machine.
- Decision:
  - Compute `SHA-256(os_machine_id|host_identity|primary_mac)` only (`fingerprintVersion` 2).
  - Remove `installation.meta` / `ServerIdentityStore` / `InstallationMeta`.
  - Omit `installationId` from offline official-request JSON; keep writing `fingerprint.meta` as a local TEMPORARY cache.
- Rationale: Same-host product reset and multi-product same-machine binding use one stable host hash; OFFICIAL binding remains the embedded `serverFingerprint`.
- Consequences: Breaking for v1 hashes — re-activate TEMPORARY and re-request/re-issue OFFICIAL. Weaker isolation for containers that share host identity inputs. Orphan `installation.meta` files are ignored.
- Date: 2026-08-05

## ADR-009: Fingerprint compute from hns-license-lib

- Status: Accepted
- Context: Other products need the same host fingerprint algorithm for OFFICIAL binding.
- Decision: Consume `ServerFingerprintProvider` from `hns-license-lib` 1.0.3+. Use `ServerFingerprint(value, version)` from `compute()` when writing `fingerprint.meta`. Keep local binding enforcement / offline request stores in this service.
- Rationale: Matches shared-runtime ADR-007; one formula across products.
- Consequences: Refresh the file JAR (or Maven coordinates) when the library fingerprint algorithm changes.
- Date: 2026-08-05

## ADR-010: Align product claim gates with OTORAS consumer

- Status: Superseded by ADR-011
- Context: `OTORAS-Backend` already required `devices_limit`, `database_limit`, and `features.i_service` on activate, exposed feature-aware status, and avoided logging payload/fingerprint. assets_management only enforced devices and logged activate binding details at INFO.
- Decision:
  - On activate, require `limits.devices_limit`, `limits.database_limit`, and a present `features.i_service` key (true or false).
  - Return `features` on license status; expose `assertCanCreateDbEquipment(long)` (caller-supplied count) and `assertIServiceFeature()` / `isIServiceLicensed()` for runtime gates.
  - Keep `assertCanCreateDevice()` wired to device create; do not invent a DB-equipment domain yet.
  - Rely on `LicenseFileReader` for `.lic` + `HNS.` validation; do not log payload or host fingerprint at INFO on activate.
  - Do not annotate read-only license status/assert/resolve paths with `@Transactional`.
- Rationale: Same signed claim contract across product consumers; safer ops logs; clearer transactional boundaries.
- Consequences: Licenses missing the new claims cannot activate until re-issued. Future DB-equipment / I-Service call sites must call the new assert APIs.
- Date: 2026-08-11
- Updated: 2026-08-11 — superseded by ADR-011 (device-only claim allowlist).

## ADR-011: Device-only claim allowlist for assets_management

- Status: Superseded by ADR-013
- Context: A single signed `.lic` may carry OTORAS claims (`database_limit`, `features.i_service`) that assets_management does not use. Activating a foreign product license would incorrectly authorize this service.
- Decision:
  - On activate, require exactly `limits.devices_limit` (no other limit keys).
  - Require empty `features` (reject any feature key, including `i_service`).
  - Keep safer activate hygiene from ADR-010 (`.lic`/`HNS.` via `LicenseFileReader`, no payload/fingerprint INFO logs, no read-only `@Transactional`).
  - Do not expose database / I-Service assert APIs in this service.
- Rationale: Each product consumer only accepts licenses that match its claim contract.
- Consequences: OTORAS-style licenses with extra claims cannot activate here. Issuer must issue assets_management licenses with only `devices_limit` and no features.
- Date: 2026-08-11

## ADR-012: Device import create-or-skip with license gate

- Status: Accepted (extended by ADR-013)
- Context: Operators need bulk device registration without bypassing `limits.devices_limit`. OTORAS equipment file integration uses create-or-skip by name.
- Decision:
  - Add `POST /api/v1/devices/import` with JSON rows `{ name, ipAddress? }`.
  - Skip when an ACTIVE device already has the same name (case-insensitive).
  - Reject duplicate names within the same request with 409.
  - Call `assertCanCreateDevice()` before each new insert; put license/validation failures in per-row `errors`.
  - Keep CSV parsing in the admin UI; API accepts validated rows only.
- Rationale: Matches OTORAS import semantics and reuses the existing create license gate (ADR-002).
- Consequences: Mid-batch limit hits leave earlier creates in place and report remaining rows as errors. INACTIVE same-name rows do not block import.
- Date: 2026-08-12
- Updated: 2026-08-12 — import also requires `features.import_feature = true` (ADR-013).

## ADR-013: Allow `import_feature` in assets_management claim allowlist

- Status: Accepted
- Context: Product schema now defines `DEVICES_LIMIT` (NUMBER) and `IMPORT_FEATURE` (BOOLEAN), projected to `limits.devices_limit` and `features.import_feature`. ADR-011 rejected all features, which blocks legitimate assets_management licenses.
- Decision:
  - On activate, require exactly `limits.devices_limit` and a present `features.import_feature` key (true or false).
  - Reject any other limit or feature keys.
  - Expose `features` on license status (already on `LicenseInfoResponse`).
  - Add `assertCanImportDevices()`; device import calls it before processing rows. Import is allowed only when `features.import_feature` is `true`.
  - Keep `assertCanCreateDevice()` for single create and per-row import inserts.
- Rationale: Aligns with issuer claim schema (`IMPORT_FEATURE` / `DEVICES_LIMIT`) and shared runtime `hasFeature()` contract.
- Consequences: Licenses without `import_feature` cannot activate. Licenses with `import_feature: false` activate but cannot import. OTORAS-style extra claims still rejected.
- Date: 2026-08-12

## ADR-014: In-repo local-dev Compose (OTORAS-Backend style)

- Status: Accepted
- Context: Operators wanted `docker-compose.yml` in the application repo, matching `OTORAS-Backend/docker-compose.yml`, instead of only the sibling `assets-management-docker/` ops folder.
- Decision: Keep a local-dev Compose file at this repo root with MariaDB (named volume, healthcheck, host port 3308) and the backend built from the existing `Dockerfile` (host port 8082). Persist license files on a named volume. Document env in `.env.example`. Leave the nginx HTTPS stack (port 9005) in `assets-management-docker/` (ADR-005).
- Rationale: Same developer workflow as OTORAS (compose next to the app). The ops folder still covers IP-based HTTPS and image transfer.
- Consequences: Two Compose entry points exist. Host port 3308 may collide with OTORAS MariaDB; override `ASSETS_DB_PORT`. `LICENSE_SIGNING_PUBLIC_KEY` must be supplied in `.env`.
- Date: 2026-08-13
