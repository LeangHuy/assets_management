package com.hunesion.assets_management.license.fingerprint;

import java.time.Instant;

/**
 * Fingerprint bound at license activation, stored beside the active {@code .lic} file.
 */
public record FingerprintMeta(
        String serverFingerprint,
        int fingerprintVersion,
        Instant computedAt,
        String payloadHash
) {
}
