package com.hunesion.assets_management.license.dto;

import java.time.Instant;

/**
 * Offline official-license request file exchanged with the License Management System.
 * Shape must match issuer {@code ActivationRequestFile} ({@code formatVersion} 1).
 * Customer, product, expiry, and claims are intentionally omitted — the issuer resolves
 * them from the demo license in its database.
 */
public record OfficialLicenseRequest(
        int formatVersion,
        String requestId,
        DemoLicense demoLicense,
        Installation installation,
        Instant requestedAt
) {
    public static final int CURRENT_FORMAT_VERSION = 1;

    public record DemoLicense(
            String licenseId,
            String licenseNumber,
            String payloadHash
    ) {
    }

    public record Installation(
            String installationId,
            String serverFingerprint,
            int fingerprintVersion,
            Instant fingerprintComputedAt
    ) {
    }
}
