package com.hunesion.assets_management.license.crypto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

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
        "features",
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
        Map<String, Integer> limits,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<String> features,
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
        limits = Collections.unmodifiableSortedMap(new TreeMap<>(limits));
        features = features == null ? List.of() : List.copyOf(features);
    }

    public int devices() {
        Integer value = limits.get("devices");
        return value == null ? 0 : value;
    }
}
