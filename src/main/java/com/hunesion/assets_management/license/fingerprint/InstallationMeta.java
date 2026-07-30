package com.hunesion.assets_management.license.fingerprint;

import java.time.Instant;

/**
 * Persistent installation identity stored beside the active {@code .lic} file.
 */
public record InstallationMeta(
        String installationId,
        int fingerprintVersion,
        Instant createdAt
) {
}
