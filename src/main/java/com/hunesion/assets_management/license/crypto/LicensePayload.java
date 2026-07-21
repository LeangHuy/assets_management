package com.hunesion.assets_management.license.crypto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@JsonPropertyOrder({
        "version",
        "licenseId",
        "licenseNumber",
        "product",
        "customerId",
        "customerCode",
        "licenseType",
        "issuedAt",
        "expiresAt",
        "limits",
        "keyId"
})
public record LicensePayload(
        int version,
        String licenseId,
        String licenseNumber,
        String product,
        String customerId,
        String customerCode,
        String licenseType,
        Instant issuedAt,
        LocalDate expiresAt,
        Limits limits,
        String keyId
) {

    public static final int CURRENT_VERSION = 1;

    public LicensePayload {
        Objects.requireNonNull(licenseId, "licenseId");
        Objects.requireNonNull(licenseNumber, "licenseNumber");
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(customerCode, "customerCode");
        Objects.requireNonNull(licenseType, "licenseType");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        Objects.requireNonNull(limits, "limits");
        Objects.requireNonNull(keyId, "keyId");
    }

    @JsonPropertyOrder({"devices"})
    public record Limits(int devices) {
        public Limits {
            if (devices < 0) {
                throw new IllegalArgumentException("devices must be >= 0");
            }
        }
    }
}
