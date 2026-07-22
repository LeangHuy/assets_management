# Current Task

## Task: Device status + recovery

### Goal

- Restore device `status` (`ACTIVE` / `INACTIVE`).
- Soft-delete on `DELETE` (set `INACTIVE`).
- Add `POST /api/v1/devices/{id}/recover` to restore `ACTIVE` under the license limit.
- Count **all** devices (active + Recycle Bin) toward the license device limit.

### Implementation result (2026-07-22)

- **Status:** column restored in `shcema.sql`; `migration_v3_device_status.sql`
- **Delete:** soft-deactivate via `updateStatus(INACTIVE)`
- **Recover:** `DeviceController` / `DeviceService.recover` (no seat check; row already counted)
- **License:** `countAllDevice()` in `assertCanCreateDevice`

### Verification

- `./gradlew compileJava` — pass
- Apply `migration_v3_device_status.sql` on DBs missing the `status` column
