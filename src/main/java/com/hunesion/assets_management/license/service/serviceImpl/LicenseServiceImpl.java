package com.hunesion.assets_management.license.service.serviceImpl;

import com.hunesion.assets_management.common.exception.ApiException;
import com.hunesion.assets_management.device.repository.DeviceRepository;
import com.hunesion.assets_management.license.crypto.LicensePayload;
import com.hunesion.assets_management.license.crypto.LicensePayloadValidator;
import com.hunesion.assets_management.license.crypto.SignedLicenseVerifier;
import com.hunesion.assets_management.license.domain.LicenseRuntimeStatus;
import com.hunesion.assets_management.license.dto.LicenseInfoResponse;
import com.hunesion.assets_management.license.service.LicenseService;
import com.hunesion.assets_management.license.storage.LicenseFileStore;
import com.hunesion.assets_management.license.support.LicenseFileReader;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class LicenseServiceImpl implements LicenseService {

    private final SignedLicenseVerifier signedLicenseVerifier;
    private final LicensePayloadValidator payloadValidator;
    private final LicenseFileStore licenseFileStore;
    private final DeviceRepository deviceRepository;
    private final LicenseFileReader licenseFileReader;
    private final Clock clock;

    @Override
    @Transactional
    public LicenseInfoResponse activateFromFile(MultipartFile file) {
        return activateLicenseKey(licenseFileReader.readLicenseKey(file));
    }

    @Override
    @Transactional
    public LicenseInfoResponse status() {
        return resolveLicense().toResponse();
    }

    @Override
    @Transactional
    public void assertCanCreateDevice() {
        ResolvedLicense resolved = resolveLicense();
        LicenseInfoResponse info = resolved.toResponse();

        if (!info.present()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "No active license. Upload and activate a .lic file before registering devices");
        }
        if (LicenseRuntimeStatus.INVALID.equals(info.status())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "License file is invalid or has been tampered with. Upload a valid .lic file");
        }
        if (LicenseRuntimeStatus.EXPIRED.equals(info.status())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "License has expired");
        }
        if (!LicenseRuntimeStatus.ACTIVE.equals(info.status())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "License is not active");
        }

//        long deviceCount = deviceRepository.countByStatus(DeviceStatus.ACTIVE);
        long deviceCount = deviceRepository.countAllDevice();
        if (deviceCount >= info.deviceLimit()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Device limit reached (" + info.deviceLimit()
                            + "). Active and Recycle Bin devices both count toward the license. "
                            + "Cannot register more devices");
        }
    }

    private LicenseInfoResponse activateLicenseKey(String licenseKey) {
        SignedLicenseVerifier.VerifiedLicense verified = signedLicenseVerifier.verify(licenseKey);
        LicensePayload payload = verified.payload();
        payloadValidator.validateForActivation(payload);

        licenseFileStore.write(verified.licenseKey());

        return toResponse(verified, LicenseRuntimeStatus.ACTIVE);
    }

    /**
     * Always reads the on-disk license file and re-verifies the Ed25519 signature before
     * returning limits or expiry.
     */
    private ResolvedLicense resolveLicense() {
        if (!licenseFileStore.exists()) {
            return ResolvedLicense.missing();
        }

        String licenseKey;
        try {
            licenseKey = licenseFileStore.read();
        } catch (ApiException ex) {
            return ResolvedLicense.missing();
        }

        SignedLicenseVerifier.VerifiedLicense verified;
        try {
            verified = signedLicenseVerifier.verify(licenseKey);
        } catch (ApiException ex) {
            return ResolvedLicense.invalid();
        }

        String status = resolveStatus(verified.payload());
        return ResolvedLicense.present(verified, status);
    }

    private String resolveStatus(LicensePayload payload) {
        if (payload.expiresAt().isBefore(LocalDate.now(clock))) {
            return LicenseRuntimeStatus.EXPIRED;
        }
        return LicenseRuntimeStatus.ACTIVE;
    }

    private static LicenseInfoResponse toResponse(
            SignedLicenseVerifier.VerifiedLicense verified,
            String status
    ) {
        LicensePayload payload = verified.payload();
        return new LicenseInfoResponse(
                true,
                status,
                verified.licenseKey(),
                payload.licenseType(),
                payload.expiresAt(),
                payload.limits().devices(),
                payload.keyId(),
                sha256Hex(verified.payloadBytes()),
                null,
                false,
                null
        );
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private record ResolvedLicense(
            boolean present,
            String status,
            SignedLicenseVerifier.VerifiedLicense verified
    ) {
        static ResolvedLicense missing() {
            return new ResolvedLicense(false, LicenseRuntimeStatus.MISSING, null);
        }

        static ResolvedLicense invalid() {
            return new ResolvedLicense(true, LicenseRuntimeStatus.INVALID, null);
        }

        static ResolvedLicense present(
                SignedLicenseVerifier.VerifiedLicense verified,
                String status
        ) {
            return new ResolvedLicense(true, status, verified);
        }

        LicenseInfoResponse toResponse() {
            if (!present) {
                return LicenseInfoResponse.missing();
            }
            if (verified == null) {
                return new LicenseInfoResponse(
                        true,
                        status,
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
            return LicenseServiceImpl.toResponse(verified, status);
        }
    }
}
