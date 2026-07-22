package com.hunesion.assets_management.license.crypto;

import com.hunesion.assets_management.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;

@Component
public class LicensePayloadValidator {

    private static final Set<String> ALLOWED_TYPES = Set.of("OFFICIAL", "TEMPORARY");

    private final Clock clock;

    public LicensePayloadValidator(Clock clock) {
        this.clock = clock;
    }

    public void validateForActivation(LicensePayload payload) {
        if (payload.version() != LicensePayload.CURRENT_VERSION) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported license payload version: " + payload.version());
        }
        if (!ALLOWED_TYPES.contains(payload.licenseType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid licenseType: " + payload.licenseType());
        }
        if (payload.limits() == null || payload.limits().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "limits must not be empty");
        }
        for (var entry : payload.limits().entrySet()) {
            if (entry.getValue() == null || entry.getValue() < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "limits." + entry.getKey() + " must be >= 0");
            }
        }
        if (payload.expiresAt().isBefore(LocalDate.now(clock))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "License has expired");
        }
    }
}
