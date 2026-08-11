package com.hunesion.assets_management.license.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record LicenseInfoResponse(
        boolean present,
        String status,
        String licenseKey,
        String licenseType,
        LocalDate expiresAt,
        Map<String, Integer> limits,
        Map<String, Boolean> features,
        String keyId,
        String payloadHash,
        String serverFingerprint,
        boolean serverBound,
        Instant issuedAt
) {

    public static LicenseInfoResponse missing() {
        return new LicenseInfoResponse(
                false,
                "MISSING",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null
        );
    }
}
