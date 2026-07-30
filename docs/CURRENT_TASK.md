# Current Task

## Status

Completed

## Objective

Align `official-license-request.json` export with the issuer activation-request schema used by `license-key-format` / `license-key-ui`.

## Context

The issuer expects nested `formatVersion`, `requestId`, `demoLicense`, and `installation` (with `fingerprintComputedAt`). Assets management still exported a flat `version` / `requestType` shape, so uploads failed client validation.

## Requirements

1. Change `OfficialLicenseRequest` to the nested issuer schema.
2. Generate a new UUID `requestId` per export.
3. Include `fingerprintComputedAt` from `FingerprintMeta.computedAt`.
4. Do not embed customer/product/expiry/claims for the issuer to trust.
5. `./gradlew compileJava` must succeed.

## Out of scope

- Issuer or license-key-ui changes
- Online activation registry

## Acceptance criteria

- Downloaded JSON validates against the issuer activation-request schema.
- File still named `official-license-request.json`.

## Relevant files

- `license/dto/OfficialLicenseRequest.java`
- `license/service/serviceImpl/LicenseServiceImpl.java`
- `license/fingerprint/OfficialLicenseRequestStore.java`

## Risks and constraints

- Existing on-disk request JSON files are the old flat shape and must be regenerated.

## Implementation result

### Status

Completed on 2026-07-30.

### Files changed

- `OfficialLicenseRequest.java` — nested `formatVersion` / `requestId` / `demoLicense` / `installation`
- `LicenseServiceImpl.java` — builds new shape; UUID `requestId`; `fingerprintComputedAt` from binding
- `OfficialLicenseRequestStore.java` — log fields updated
- `docs/DECISIONS.md` — ADR-004 updated
- `docs/CURRENT_TASK.md` / `docs/WORK_REPORT.md`

### Verification

- `./gradlew compileJava` — Passed
- `./gradlew test` — Passed (no test sources)

### Known limitations

- Existing flat `official-license-request.json` files on disk must be regenerated via the Generate Official License Request action.
- Old `fingerprint.meta` without `computedAt` still requires re-activation.

### Remaining work

- None for this alignment task.
