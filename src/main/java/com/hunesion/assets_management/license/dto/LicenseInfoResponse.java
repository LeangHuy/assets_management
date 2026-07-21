package com.hunesion.assets_management.license.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LicenseInfoResponse(
        boolean present,
        String status,
        String licenseKey,
        String licenseType,
        LocalDate expiresAt,
        Integer deviceLimit,
        String keyId,
        String payloadHash,
        String serverFingerprint,
        boolean serverBound,
        LocalDateTime activatedAt
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
                false,
                null
        );
    }
}
