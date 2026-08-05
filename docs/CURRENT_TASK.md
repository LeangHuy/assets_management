# Current Task

## Status

Completed

## Objective

Remove `installationId` from server fingerprint binding so fingerprints are host-only (`os_machine_id|host_identity|primary_mac`).

## Context

Operators want existing OFFICIAL licenses to remain usable after a product data reset on the same host, and the same host fingerprint across products without sharing `installation.meta`.

## Requirements

1. Change fingerprint formula to exclude `installation_id`; bump `FINGERPRINT_VERSION` to 2.
2. Delete `installation.meta` / `ServerIdentityStore` / `InstallationMeta`.
3. Remove `installationId` from offline `OfficialLicenseRequest.Installation`.
4. Update ADR / architecture docs.
5. `./gradlew compileJava` passes.

## Out of scope

- Extracting fingerprint compute into `hns-license-lib` (separate follow-up).
- Migrating already-issued OFFICIAL licenses (must re-request / re-issue).

## Acceptance criteria

1. Activate / status / official-request no longer read or write `installation.meta`.
2. Offline request JSON has no `installationId`.
3. Compile succeeds.

## Implementation result

### Status

Completed on 2026-08-05.

### Files changed

- `ServerFingerprintProvider.java`: host-only hash; `FINGERPRINT_VERSION = 2`.
- Deleted `InstallationMeta.java`, `ServerIdentityStore.java`.
- `OfficialLicenseRequest.Installation`: removed `installationId`.
- `LicenseServiceImpl.java`: no identity store usage.
- `OfficialLicenseRequestStore.java`: log without installationId.
- ADR-008; ADR-003 superseded; ARCHITECTURE updated.

### Verification

- `./gradlew compileJava` — Passed

### Known limitations

- Existing OFFICIAL licenses with v1 (installation_id in hash) fail binding until re-issued.
- Orphan `installation.meta` files on disk are ignored.
- Containers sharing host machine-id/MAC get the same fingerprint.

### Remaining work

- Redeploy product + issuer + UI; re-activate TEMPORARY and re-request/re-issue OFFICIAL for each bound server.
- Optional: extract fingerprint compute into `hns-license-lib`.
