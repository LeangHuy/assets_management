# Agent Instructions

## Project stack

- Java 21
- Spring Boot 4 (Web MVC, JDBC, Validation)
- MyBatis
- MariaDB
- springdoc OpenAPI
- Lombok
- `hns-license-lib` (`libs/hns-license-lib-1.0.1.jar`) for Ed25519 license verify/storage

## Required workflow

Before implementing:

1. Read `docs/ARCHITECTURE.md`.
2. Read `docs/DECISIONS.md`.
3. Read `docs/CURRENT_TASK.md`.
4. Inspect existing patterns before creating new abstractions.
5. Do not change unrelated files.
6. Do not run destructive database or infrastructure commands unless explicitly requested.
7. Run compilation and relevant tests (`./gradlew compileJava`, `./gradlew test` when applicable).
8. Update `docs/CURRENT_TASK.md` with the implementation result.

## Code standards

- Prefer clear domain packages (`device`, `license`, `common`).
- Keep API DTOs separate from persistence records.
- Validate request payloads; return consistent `ApiResponse` / `ApiException` errors.
- Do not commit secrets, private keys, or real `.env` credentials.
- Prefer additive SQL migrations under `src/main/resources/schema/`.
- Hold the Ed25519 **public key only**; never ship or log the issuer private key.
- Product-specific claim enforcement (e.g. `limits.devices_limit`) stays in this service, not in `hns-license-lib`.

## Verification commands

```bash
./gradlew compileJava
./gradlew test
```
