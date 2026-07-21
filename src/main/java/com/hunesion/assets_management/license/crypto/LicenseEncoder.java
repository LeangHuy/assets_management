package com.hunesion.assets_management.license.crypto;

import com.hunesion.assets_management.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class LicenseEncoder {

    public static final String PREFIX = "HNS";
    private static final int SEGMENT_COUNT = 3;

    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder decoder = Base64.getUrlDecoder();

    public String encode(byte[] payloadBytes, byte[] signatureBytes) {
        return PREFIX
                + "."
                + encoder.encodeToString(payloadBytes)
                + "."
                + encoder.encodeToString(signatureBytes);
    }

    public EncodedLicense decode(String licenseKey) {
        if (licenseKey == null || licenseKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "License key is required");
        }

        String trimmed = licenseKey.trim();
        String[] parts = trimmed.split("\\.", -1);
        if (parts.length != SEGMENT_COUNT) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid license format: expected " + SEGMENT_COUNT + " segments");
        }
        if (!PREFIX.equals(parts[0])) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid license format: prefix must be " + PREFIX);
        }
        if (parts[1].isBlank() || parts[2].isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid license format: empty payload or signature");
        }

        try {
            byte[] payloadBytes = decoder.decode(parts[1]);
            byte[] signatureBytes = decoder.decode(parts[2]);
            return new EncodedLicense(trimmed, payloadBytes, signatureBytes);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid license format: malformed Base64Url");
        }
    }

    public record EncodedLicense(String raw, byte[] payloadBytes, byte[] signatureBytes) {
    }
}
