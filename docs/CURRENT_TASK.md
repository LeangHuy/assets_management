# Current Task

## Task: File-based license storage with verify-on-read

### Goal

Store the signed `.lic` file on disk as the single source of truth for license claims. Re-verify Ed25519 signature on every status check and device enforcement. Use signature validation + expiry + device limit only.

### Architecture

```text
data/license/active.lic     ← signed HNS wire string (canonical)
```

On every `GET /license` and `POST /devices`:
1. Read `active.lic` from disk
2. Verify Ed25519 signature
3. Parse limits/expiry from verified payload (never trust DB columns for claims)

### Status values

| Status | Meaning |
|--------|---------|
| `MISSING` | No `active.lic` file |
| `ACTIVE` | Valid signature and not expired |
| `EXPIRED` | Valid signature but past `expiresAt` |
| `INVALID` | File present but signature verification failed (tampered) |

### Migration

| Script | When |
|--------|------|
| `migration_v1_license.sql` | Fresh upgrade from device-only schema |
| `migration_v2_license_file.sql` | Upgrade from DB `active_license` → file storage |

After `migration_v2_license_file.sql`, re-upload the `.lic` file.

### Config

```properties
LICENSE_STORAGE_PATH=data/license/active.lic
LICENSE_SIGNING_PUBLIC_KEY_PATH=data/signing/ed25519-2026-01.public.b64
```

Mount `data/license/` as a persistent volume in Docker.

### Verification

- `./gradlew test` — pass
- Tampering `active.lic` payload without valid signature → status `INVALID`, device create blocked
