# Architecture Decisions

## ADR-001: Shared license runtime via file JAR

- Status: Accepted
- Context: Ed25519 verify/store logic was duplicated across product services.
- Decision: Depend on `libs/hns-license-lib-1.0.1.jar` (`com.hunesion.license.runtime.*`) for wire decode, signature verify, payload validation, and `LicenseFileStore`. Keep product claim enforcement in this service.
- Rationale: Matches `account-management` and avoids re-copying crypto/storage code.
- Consequences: After library changes, rebuild/copy the JAR into `libs/` (or switch to `mavenLocal()` coordinates). Configure `LICENSE_SIGNING_PUBLIC_KEY` and `LICENSE_STORAGE_PATH`.
- Date: 2026-07-24

## ADR-002: Device limit from signed `limits.devices_limit`

- Status: Accepted
- Context: Asset registration must not exceed the licensed device cap.
- Decision: On activate, require `limits.devices_limit`. On create device, count all devices (including recycle-bin statuses) and reject with 409 when at or above the limit.
- Rationale: Claim values live only in the signed payload; the product owns enforcement.
- Consequences: Licenses without `devices_limit` cannot activate on this service.
- Date: 2026-07-24

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
- Consequences: Operators must set `PUBLIC_HOST` / `CORS_ALLOWED_ORIGINS` to the IP or hostname clients use; browsers warn on self-signed certs until a trusted cert is installed; sibling checkouts of `assets_management` and `assets-management-ui` are required next to `assets-management-docker`.
- Date: 2026-07-30

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
