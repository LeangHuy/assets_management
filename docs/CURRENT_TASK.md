# Current Task

## Task: Signing config — public key only

### Goal

- Remove unused `license.signing.key-id`.
- Keep only `license.signing.public-key` / `LICENSE_SIGNING_PUBLIC_KEY` for Ed25519 verification.

### Implementation result (2026-07-22)

- Removed `key-id` from `application.yml`, `SigningKeyProperties`, and `PublicKeyProvider`.
- License payload still carries `keyId` from the issuer; API responses use that.

### Verification

- `./gradlew compileJava`
