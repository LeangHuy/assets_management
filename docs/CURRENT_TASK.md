# Current Task

## Status

Completed

## Objective

Enforce issuer-embedded `serverFingerprint` on activate and status so an OFFICIAL `.lic` cannot activate on a different server.

## Context

Local `fingerprint.meta` alone rebinds on each activate. OFFICIAL licenses now carry the fingerprint in the signed payload from `license-key-format`.

## Requirements

1. Refresh `libs/hns-license-lib-1.0.0.jar` with optional `serverFingerprint`.
2. On activate: if payload fingerprint is present and mismatches this host, reject with 403.
3. On resolve: payload mismatch yields `BINDING_INVALID` even if local meta was rewritten.
4. TEMPORARY (null fingerprint) keeps local bind-on-activate behavior.
5. `./gradlew compileJava` succeeds.

## Out of scope

- account-management fingerprint enforcement
- UI redesign
- Forcing re-issue of old unbound OFFICIAL files

## Acceptance criteria

- Matching OFFICIAL activates and stays ACTIVE.
- Same OFFICIAL on another server is rejected / `BINDING_INVALID`.
- TEMPORARY demos still activate on any host.

## Relevant files

- `libs/hns-license-lib-1.0.0.jar`
- `license/service/serviceImpl/LicenseServiceImpl.java`

## Risks and constraints

- Docker must rebuild/redeploy with the new JAR.
- Old OFFICIAL licenses without the field remain portable until re-issued.

## Implementation result

### Status

Completed on 2026-07-31.

### Files changed

- `LicenseServiceImpl.java` — payload fingerprint check on activate and resolve
- `libs/hns-license-lib-1.0.0.jar` — refreshed
- `docs/DECISIONS.md` — ADR-006

### Verification

- `./gradlew compileJava` — Passed

### Known limitations

- Already-issued OFFICIAL licenses without embedded fingerprint still activate on any server.

### Remaining work

- Redeploy both Docker hosts and re-issue OFFICIAL after a new activation request to verify cross-server rejection.
