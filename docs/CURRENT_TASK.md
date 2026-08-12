# Current Task

## Status

Completed

## Objective

Add bulk device import (`POST /api/v1/devices/import`) that create-or-skips by active name and enforces licensed `limits.devices_limit` on each new create.

## Context

Single-device create already called `assertCanCreateDevice()`. Operators need CSV-driven bulk registration without bypassing the device cap. Import mirrors OTORAS equipment file-integration semantics (create-or-skip).

## Requirements

1. Accept a JSON body `{ "devices": [ { "name", "ipAddress?" } ] }`.
2. Reject duplicate names within the same request (409).
3. Skip rows that already have an ACTIVE device with the same name (case-insensitive).
4. Call `assertCanCreateDevice()` before each new insert; report per-row failures in `errors`.
5. Return `{ created, createdCount, skippedCount, errors }`.

## Out of scope

- Multipart CSV upload on the API (UI parses CSV client-side).
- Unique DB constraint on device name.
- Hard-failing the whole import when the license limit is reached mid-batch.

## Acceptance criteria

- Import creates new ACTIVE devices under the license limit.
- Existing active names are skipped, not overwritten.
- When the limit is reached, further rows appear in `errors` with the limit message.
- Duplicate names in one request return 409.
- `./gradlew compileJava` succeeds.

## Relevant files

- `device/controller/DeviceController.java`
- `device/service/DeviceServiceImpl.java`
- `device/dto/DeviceImport*.java`
- `device/repository/DeviceRepository.java`
- `docs/DECISIONS.md`, `docs/ARCHITECTURE.md`

## Risks and constraints

- Recycle Bin devices already count toward the limit (ADR-002); import cannot create more until space frees or the license increases.
- INACTIVE devices with the same name do not block import (same as OTORAS active-only skip).

## Implementation result

### Status

Completed on 2026-08-12.

### Files changed

- `DeviceImportRequest.java`, `DeviceImportRow.java`, `DeviceImportResult.java`, `DeviceImportErrorItem.java`: import DTOs.
- `DeviceRepository.java`: `findActiveByNameIgnoreCase`.
- `DeviceService.java` / `DeviceServiceImpl.java`: `importDevices` create-or-skip + license gate.
- `DeviceController.java`: `POST /api/v1/devices/import`.
- `docs/DECISIONS.md`: ADR-012.
- `docs/ARCHITECTURE.md`: import data-flow note.

### Verification

- `./gradlew compileJava --rerun-tasks` — Passed

### Known limitations

- No DB unique index on name; skip is ACTIVE-only.
- Limit mid-import yields per-row errors rather than rolling back earlier creates in the batch (same transaction commits successful inserts).

### Remaining work

- Optional: unique name constraint / skip INACTIVE names as well.
