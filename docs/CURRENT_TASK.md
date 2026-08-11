# Current Task

## Status

Completed

## Objective

Enforce an assets_management-only claim allowlist on activate: require exactly `limits.devices_limit`, reject any other limit keys, and require empty `features` (so licenses with `i_service` or other product claims cannot activate).

## Context

Earlier OTORAS-style work required `database_limit` and `features.i_service`. Product policy for this service is device-only: a `.lic` built for OTORAS (devices + i_service / database) must be rejected here.

## Requirements

1. On activate, require `limits.devices_limit`.
2. Reject activate when `limits` contains any key other than `devices_limit`.
3. Reject activate when `features` is non-empty.
4. Remove unused `assertCanCreateDbEquipment` / `assertIServiceFeature` / `isIServiceLicensed` APIs.
5. Keep safer activate hygiene (no payload/fingerprint INFO logs; `.lic`/`HNS.` via `LicenseFileReader`; no read-only `@Transactional`).
6. Update ADR-010 / architecture docs to match.

## Out of scope

- Product-code matching (`payload.product`) unless already required by the lib validator.
- UI changes.

## Acceptance criteria

- License with only `devices_limit` and empty features activates.
- License with `devices_limit` + `i_service` is rejected.
- License with `devices_limit` + `database_limit` is rejected.
- `./gradlew compileJava` succeeds.

## Relevant files

- `LicenseService.java`, `LicenseServiceImpl.java`, `LicenseInfoResponse.java`
- `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `docs/CURRENT_TASK.md`

## Risks and constraints

- Existing activated OTORAS-style licenses cannot be re-activated on this service (intended).

## Implementation result

### Status

Completed on 2026-08-11.

### Files changed

- `LicenseServiceImpl.java`: `requireAssetManagementClaims()` allowlists only `devices_limit` and empty `features`.
- `LicenseService.java`: removed DB / I-Service assert APIs.
- `docs/DECISIONS.md`: ADR-010 superseded; ADR-011 accepted.
- `docs/ARCHITECTURE.md` / `AGENTS.md`: device-only claim contract.

### Verification

- `./gradlew compileJava --rerun-tasks` — Passed

### Known limitations

- Status still reports whatever is already on disk; claim allowlist is enforced on activate, not on every status read.
- `payload.product` is not yet used as an additional reject rule.

### Remaining work

- Optionally also require `payload.product` to match the assets_management product code.
