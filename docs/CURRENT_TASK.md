# Current Task

## Status

Completed

## Objective

Allow SRA to generate an offline license request from an ACTIVE or EXPIRED OFFICIAL license (`requestType: RENEWAL`).

## Context

`generateOfficialLicenseRequest` previously required TEMPORARY only.

## Requirements

1. TEMPORARY + ACTIVE → `requestType: CONVERSION`.
2. OFFICIAL + ACTIVE or EXPIRED + bound → `requestType: RENEWAL`.
3. Include `requestType` in the JSON (formatVersion 1).
4. Compile succeeds.

## Out of scope

- Issuer approve/supersede
- UI beyond assets-management-ui

## Implementation result

### Status

Completed on 2026-08-04.

### Files changed

- `OfficialLicenseRequest.java`: added `requestType`.
- `LicenseServiceImpl.generateOfficialLicenseRequest`: CONVERSION vs RENEWAL guards.
- Controller/docs updated (ADR-007).

### Verification

- `./gradlew compileJava` — Passed

### Known limitations

- Issuer must be updated (ADR-016) to accept RENEWAL files.

### Remaining work

- Redeploy with issuer + UI and verify renewal E2E.
