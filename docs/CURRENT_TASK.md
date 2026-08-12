# Current Task

## Status

Completed

## Objective

Align assets_management license activation and enforcement with product claims `DEVICES_LIMIT` and `IMPORT_FEATURE` (`limits.devices_limit`, `features.import_feature`).

## Context

ADR-011 required empty `features`, but the issuer now issues `IMPORT_FEATURE` for assets_management. Device import must be gated by `features.import_feature = true`; device create remains gated by `limits.devices_limit`.

## Requirements

1. On activate, require `limits.devices_limit` and a present `features.import_feature` key.
2. Reject activate when `limits` or `features` contain any key outside the allowlist.
3. Add `assertCanImportDevices()` using `payload.hasFeature("IMPORT_FEATURE")`.
4. Call `assertCanImportDevices()` at the start of device import.
5. Keep `assertCanCreateDevice()` for single create and per-row import inserts.

## Out of scope

- UI changes to hide Import when `import_feature` is false (backend enforcement only).

## Acceptance criteria

- License with `devices_limit` + `import_feature: true` activates; import allowed.
- License with `import_feature: false` activates; import returns 403.
- License missing `import_feature` or with extra claims is rejected on activate.
- `./gradlew compileJava` succeeds.

## Relevant files

- `license/service/LicenseService.java`, `license/service/serviceImpl/LicenseServiceImpl.java`
- `device/service/DeviceServiceImpl.java`
- `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`, `AGENTS.md`

## Implementation result

### Status

Completed on 2026-08-12.

### Files changed

- `LicenseServiceImpl.java`: allowlist `import_feature`; `assertCanImportDevices()`.
- `LicenseService.java`: new assert API.
- `DeviceServiceImpl.java`: import calls `assertCanImportDevices()` first.
- `DeviceController.java`: OpenAPI note for import feature.
- `docs/DECISIONS.md`: ADR-013; ADR-011 superseded.

### Verification

- `./gradlew compileJava --rerun-tasks` — Passed

### Known limitations

- UI still shows Import even when `import_feature` is false (API returns 403).

### Remaining work

- Optionally hide Import in `assets-management-ui` from license status `features`.
