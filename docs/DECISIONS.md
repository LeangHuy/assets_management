# Architecture Decisions

## ADR-001: Shared license runtime via file JAR

- Status: Accepted
- Context: Ed25519 verify/store logic was duplicated across product services.
- Decision: Depend on `libs/hns-license-lib-1.0.0.jar` (`com.hunesion.license.runtime.*`) for wire decode, signature verify, payload validation, and `LicenseFileStore`. Keep product claim enforcement in this service.
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

- Status: Accepted
- Context: A license must be usable only on the server where it was activated. MAC-only binding is unstable under Docker/VMs.
- Decision: At activate, ensure a persistent `installation_id`, compute `SHA-256(installation_id|os_machine_id|host_identity|primary_mac)`, and store the hash in `fingerprint.meta` beside the `.lic`. On status and device-create, recompute and compare; mismatch returns `BINDING_INVALID` and blocks device creation. Persist identity/binding as file sidecars under the license storage directory (not new DB tables).
- Rationale: Matches the local `/data/.../license/` layout, survives container recreate when the storage path is volume-mounted, and does not require online access to the issuer.
- Consequences: Hardware replacement or lost volume invalidates binding until the operator re-activates the same `.lic`. Issuer-side registry and Activation Certificates remain future work.
- Date: 2026-07-30

## ADR-004: Future issuer binding via offline files only

- Status: Accepted
- Context: The full license diagram includes an activation request to the issuer and a signed activation certificate. Network access from customer servers to the issuer may be unavailable.
- Decision: Do not add an online `ActivationRegistryClient` in Phase A. When issuer binding is added later, use offline file export/import only.
- Rationale: Aligns with air-gapped customer deployments and avoids coupling activate to issuer availability.
- Consequences: Phase A cannot prevent the same `.lic` from being activated on two servers that never sync with the issuer; that gap is accepted until offline issuer binding ships.
- Date: 2026-07-30

## ADR-005: Compose stack with nginx HTTPS on port 9005

- Status: Accepted
- Context: Operators need a single way to run UI, backend, and MariaDB behind HTTPS on a fixed host port, often by server IP.
- Decision: Keep app Dockerfiles in each repo, and put the full ops stack in sibling folder `assets-management-docker/` (`docker-compose.yml`, nginx, MariaDB init, env template, certs). nginx listens on container 443, published as host **9005**. UI is built with empty `NEXT_PUBLIC_API_BASE_URL` so the browser calls same-origin `/api/...` through nginx. TLS uses self-signed certs auto-generated for `PUBLIC_HOST` (replaceable with CA certs under `certs/`).
- Rationale: One deploy folder matches ops expectations; reverse-proxy avoids exposing backend/UI ports and keeps CORS/TLS at the edge; source repos stay free of compose/certs clutter.
- Consequences: Operators must set `PUBLIC_HOST` / `CORS_ALLOWED_ORIGINS` to the IP or hostname clients use; browsers warn on self-signed certs until a trusted cert is installed; sibling checkouts of `assets_management` and `assets-management-ui` are required next to `assets-management-docker`.
- Date: 2026-07-30
