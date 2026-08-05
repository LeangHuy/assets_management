# Current Task

## Status

Completed

## Objective

Consume versioned `ServerFingerprint` from `hns-license-lib` 1.0.3.

## Context

Library `compute()` now returns hash + algorithm version together.

## Requirements

1. Depend on `hns-license-lib-1.0.3`.
2. Use `fingerprint.value()` / `fingerprint.version()` in activate and resolve.
3. Compile succeeds.

## Out of scope

- Moving binding stores into the lib.

## Implementation result

### Status

Completed on 2026-08-05.

### Files changed

- `build.gradle` / `AGENTS.md`: `hns-license-lib-1.0.3.jar`.
- `LicenseServiceImpl.java`: uses `ServerFingerprint` value/version when binding.
- ADR-001 / ADR-009 updated.

### Verification

- `./gradlew compileJava --rerun-tasks` — Passed

### Known limitations

- Older `libs/hns-license-lib-1.0.1.jar` / `1.0.2.jar` may remain on disk unused.

### Remaining work

- Clean unused older JARs when unlocked.
