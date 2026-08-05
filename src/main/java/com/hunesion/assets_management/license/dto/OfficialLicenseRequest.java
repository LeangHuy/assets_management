package com.hunesion.assets_management.license.dto;

import java.time.Instant;

/**
 * Offline official-license request file exchanged with the License Management System.
 * Shape must match issuer {@code ActivationRequestFile} ({@code formatVersion} 1).
 * Customer, product, expiry, and claims are intentionally omitted — the issuer resolves
 * them from the source license in its database.
 *
 * <p>{@code requestType} is {@code CONVERSION} (TEMPORARY → OFFICIAL) or {@code RENEWAL}
 * (OFFICIAL → new OFFICIAL). The nested {@code demoLicense} object holds the source license
 * for both kinds (wire name kept for compatibility).
 */
public record OfficialLicenseRequest(
        int formatVersion,
        String requestId,
        String requestType,
        DemoLicense demoLicense,
        Installation installation,
        Instant requestedAt
) {
    public static final int CURRENT_FORMAT_VERSION = 1;
    public static final String REQUEST_TYPE_CONVERSION = "CONVERSION";
    public static final String REQUEST_TYPE_RENEWAL = "RENEWAL";

    public record DemoLicense(
            String licenseId,
            String licenseNumber,
            String payloadHash
    ) {
    }

    public record Installation(
            String serverFingerprint,
            int fingerprintVersion,
            Instant fingerprintComputedAt
    ) {
    }
}
