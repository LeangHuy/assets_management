package com.hunesion.assets_management.license.service.serviceImpl;

import com.hunesion.assets_management.common.exception.ApiException;
import com.hunesion.assets_management.device.repository.DeviceRepository;
import com.hunesion.assets_management.license.dto.LicenseInfoResponse;
import com.hunesion.assets_management.license.dto.OfficialLicenseRequest;
import com.hunesion.assets_management.license.fingerprint.FingerprintMeta;
import com.hunesion.assets_management.license.fingerprint.LicenseBindingStore;
import com.hunesion.assets_management.license.fingerprint.OfficialLicenseRequestStore;
import com.hunesion.assets_management.license.service.LicenseService;
import com.hunesion.license.runtime.crypto.LicensePayload;
import com.hunesion.license.runtime.crypto.LicensePayloadValidator;
import com.hunesion.license.runtime.crypto.SignedLicenseVerifier;
import com.hunesion.license.runtime.domain.LicenseRuntimeStatus;
import com.hunesion.license.runtime.exception.LicenseException;
import com.hunesion.license.runtime.fingerprint.ServerFingerprint;
import com.hunesion.license.runtime.fingerprint.ServerFingerprintProvider;
import com.hunesion.license.runtime.storage.LicenseFileStore;
import com.hunesion.license.runtime.support.LicenseFileReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseServiceImpl implements LicenseService {

    private static final String LIMIT_DEVICES = "devices_limit";
    private static final String FEATURE_IMPORT = "import_feature";
    private static final Set<String> ALLOWED_LIMIT_KEYS = Set.of(LIMIT_DEVICES);
    private static final Set<String> ALLOWED_FEATURE_KEYS = Set.of(FEATURE_IMPORT);

    private final SignedLicenseVerifier signedLicenseVerifier;
    private final LicensePayloadValidator payloadValidator;
    private final LicenseFileStore licenseFileStore;
    private final DeviceRepository deviceRepository;
    private final LicenseFileReader licenseFileReader;
    private final LicenseBindingStore licenseBindingStore;
    private final OfficialLicenseRequestStore officialLicenseRequestStore;
    private final ServerFingerprintProvider serverFingerprintProvider;
    private final Clock clock;

    @Override
    public LicenseInfoResponse activateFromFile(MultipartFile file) {
        // LicenseFileReader enforces .lic extension and HNS. wire prefix before verify.
        return activateLicenseKey(licenseFileReader.readLicenseKey(file));
    }

    @Override
    public LicenseInfoResponse status() {
        return resolveLicense().toResponse();
    }

    @Override
    public void assertCanCreateDevice() {
        ResolvedLicense resolved = resolveLicense();
        LicenseInfoResponse info = requireActiveBoundLicense(resolved);

        Integer devicesLimit = info.limits() == null ? null : info.limits().get(LIMIT_DEVICES);
        if (devicesLimit == null) {
            throw new ApiException(HttpStatus.FORBIDDEN, "License is missing limits.devices_limit");
        }

        long deviceCount = deviceRepository.countAllDevice();
        if (deviceCount >= devicesLimit) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Device limit reached (" + devicesLimit
                            + "). Active and Recycle Bin devices both count toward the license. "
                            + "Cannot register more devices");
        }
    }

    @Override
    public void assertCanImportDevices() {
        ResolvedLicense resolved = resolveLicense();
        requireActiveBoundLicense(resolved);

        SignedLicenseVerifier.VerifiedLicense verified = resolved.verified();
        if (verified == null || !verified.payload().hasFeature("IMPORT_FEATURE")) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "License does not include features.import_feature. Device import is not licensed");
        }
    }

    @Override
    public LicenseRequestFile generateOfficialLicenseRequest() {
        ResolvedLicense resolved = resolveLicense();
        LicenseInfoResponse info = getLicenseInfoResponse(resolved);

        LicensePayload payload = resolved.verified().payload();
        String requestType = getRequestType(payload, info);

        FingerprintMeta binding = licenseBindingStore.read()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "fingerprint.meta is missing. Re-activate the license"));

        OfficialLicenseRequest request = new OfficialLicenseRequest(
                OfficialLicenseRequest.CURRENT_FORMAT_VERSION,
                UUID.randomUUID().toString(),
                requestType,
                new OfficialLicenseRequest.DemoLicense(
                        payload.licenseId(),
                        payload.licenseNumber(),
                        binding.payloadHash()
                ),
                new OfficialLicenseRequest.Installation(
                        binding.serverFingerprint(),
                        binding.fingerprintVersion(),
                        binding.computedAt()
                ),
                Instant.now(clock)
        );

        byte[] content = officialLicenseRequestStore.write(request);
        return new LicenseRequestFile(content, OfficialLicenseRequestStore.fileNameFor(request));
    }

    private static @NonNull LicenseInfoResponse getLicenseInfoResponse(ResolvedLicense resolved) {
        LicenseInfoResponse info = resolved.toResponse();

        if (!info.present() || resolved.verified() == null) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "No active license. Activate a .lic file before generating a license request");
        }
        if (LicenseRuntimeStatus.INVALID.equals(info.status())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "License file is invalid or has been tampered with");
        }
        if (LicenseRuntimeStatus.BINDING_INVALID.equals(info.status()) || !info.serverBound()) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "License binding is invalid for this server. Re-activate the .lic file before generating a request");
        }
        return info;
    }

    private static @NonNull String getRequestType(LicensePayload payload, LicenseInfoResponse info) {
        String licenseType = payload.licenseType() == null ? "" : payload.licenseType().trim().toUpperCase();
        String requestType;
        if ("TEMPORARY".equals(licenseType)) {
            if (LicenseRuntimeStatus.EXPIRED.equals(info.status())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "License has expired");
            }
            if (!LicenseRuntimeStatus.ACTIVE.equals(info.status())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "License is not active");
            }
            requestType = OfficialLicenseRequest.REQUEST_TYPE_CONVERSION;
        } else if ("OFFICIAL".equals(licenseType)) {
            if (!LicenseRuntimeStatus.ACTIVE.equals(info.status())
                    && !LicenseRuntimeStatus.EXPIRED.equals(info.status())) {
                throw new ApiException(HttpStatus.FORBIDDEN,
                        "Official license must be ACTIVE or EXPIRED to generate a renewal request");
            }
            requestType = OfficialLicenseRequest.REQUEST_TYPE_RENEWAL;
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "License request can only be generated from TEMPORARY or OFFICIAL licenses");
        }
        return requestType;
    }

    private LicenseInfoResponse activateLicenseKey(String licenseKey) {
        SignedLicenseVerifier.VerifiedLicense verified = signedLicenseVerifier.verify(licenseKey);
        LicensePayload payload = verified.payload();
        payloadValidator.validateForActivation(payload);
        requireAssetManagementClaims(payload);

        ServerFingerprint fingerprint = serverFingerprintProvider.compute();
        requirePayloadFingerprintMatch(payload, fingerprint.value());
        String payloadHash = sha256Hex(verified.payloadBytes());

        licenseFileStore.write(verified.licenseKey());
        licenseBindingStore.write(new FingerprintMeta(
                fingerprint.value(),
                fingerprint.version(),
                Instant.now(clock),
                payloadHash
        ));

        return toResponse(verified, LicenseRuntimeStatus.ACTIVE, fingerprint.value(), true);
    }

    /**
     * When the signed payload embeds a server fingerprint (OFFICIAL licenses),
     * it must match this host. Blank/null means unbound (TEMPORARY demos).
     */
    private static void requirePayloadFingerprintMatch(LicensePayload payload, String currentFingerprint) {
        String embedded = payload.serverFingerprint();
        if (embedded == null || embedded.isBlank()) {
            return;
        }
        if (!embedded.equalsIgnoreCase(currentFingerprint)) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "This license is bound to another server");
        }
    }

    /**
     * Asset management accepts only {@code limits.devices_limit} and {@code features.import_feature}.
     * Licenses with foreign-product claims (e.g. {@code database_limit}, {@code i_service}) are rejected.
     */
    private void requireAssetManagementClaims(LicensePayload payload) {
        Map<String, Integer> limits = payload.limits();
        if (limits == null || !limits.containsKey(LIMIT_DEVICES)) {
            throw new LicenseException(HttpStatus.BAD_REQUEST,
                    "limits.devices_limit is required for asset management");
        }

        Set<String> unexpectedLimits = limits.keySet().stream()
                .filter(key -> !ALLOWED_LIMIT_KEYS.contains(key))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!unexpectedLimits.isEmpty()) {
            throw new LicenseException(HttpStatus.BAD_REQUEST,
                    "License limits are not valid for asset management. Allowed: "
                            + ALLOWED_LIMIT_KEYS + "; unexpected: " + unexpectedLimits);
        }

        Map<String, Boolean> features = payload.features();
        if (features == null || !features.containsKey(FEATURE_IMPORT)) {
            throw new LicenseException(HttpStatus.BAD_REQUEST,
                    "features.import_feature is required for asset management");
        }

        Set<String> unexpectedFeatures = features.keySet().stream()
                .filter(key -> !ALLOWED_FEATURE_KEYS.contains(key))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!unexpectedFeatures.isEmpty()) {
            throw new LicenseException(HttpStatus.BAD_REQUEST,
                    "License features are not valid for asset management. Allowed: "
                            + ALLOWED_FEATURE_KEYS + "; unexpected: " + unexpectedFeatures);
        }
    }

    private LicenseInfoResponse requireActiveBoundLicense(ResolvedLicense resolved) {
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
        if (LicenseRuntimeStatus.BINDING_INVALID.equals(info.status())) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "License binding is invalid for this server. Re-activate the .lic file on this host");
        }
        if (!LicenseRuntimeStatus.ACTIVE.equals(info.status())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "License is not active");
        }
        return info;
    }

    /**
     * Always reads the on-disk license file and re-verifies the Ed25519 signature before
     * returning limits or expiry. Also recomputes the server fingerprint against the bound hash.
     */
    private ResolvedLicense resolveLicense() {
        if (!licenseFileStore.exists()) {
            return ResolvedLicense.missing();
        }

        String licenseKey;
        try {
            licenseKey = licenseFileStore.read();
        } catch (LicenseException ex) {
            return ResolvedLicense.missing();
        }

        SignedLicenseVerifier.VerifiedLicense verified;
        try {
            verified = signedLicenseVerifier.verify(licenseKey);
        } catch (LicenseException ex) {
            return ResolvedLicense.invalid();
        }

        Optional<FingerprintMeta> binding = licenseBindingStore.read();
        ServerFingerprint fingerprint = serverFingerprintProvider.compute();
        String currentFingerprint = fingerprint.value();

        String embeddedFingerprint = verified.payload().serverFingerprint();
        boolean payloadBindingMismatch = embeddedFingerprint != null
                && !embeddedFingerprint.isBlank()
                && !embeddedFingerprint.equalsIgnoreCase(currentFingerprint);
        boolean localBindingMismatch = binding.isEmpty()
                || binding.get().serverFingerprint() == null
                || binding.get().serverFingerprint().isBlank()
                || !binding.get().serverFingerprint().equals(currentFingerprint);

        if (payloadBindingMismatch || localBindingMismatch) {
            return ResolvedLicense.present(
                    verified,
                    LicenseRuntimeStatus.BINDING_INVALID,
                    currentFingerprint,
                    false
            );
        }

        String status = resolveStatus(verified.payload());
        return ResolvedLicense.present(verified, status, currentFingerprint, true);
    }

    private String resolveStatus(LicensePayload payload) {
        if (payload.expiresAt().isBefore(LocalDate.now(clock))) {
            return LicenseRuntimeStatus.EXPIRED;
        }
        return LicenseRuntimeStatus.ACTIVE;
    }

    private static LicenseInfoResponse toResponse(
            SignedLicenseVerifier.VerifiedLicense verified,
            String status,
            String serverFingerprint,
            boolean serverBound
    ) {
        LicensePayload payload = verified.payload();
        return new LicenseInfoResponse(
                true,
                status,
                verified.licenseKey(),
                payload.licenseType(),
                payload.expiresAt(),
                payload.limits(),
                payload.features(),
                payload.keyId(),
                sha256Hex(verified.payloadBytes()),
                serverFingerprint,
                serverBound,
                payload.issuedAt()
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
            SignedLicenseVerifier.VerifiedLicense verified,
            String serverFingerprint,
            boolean serverBound
    ) {
        static ResolvedLicense missing() {
            return new ResolvedLicense(false, LicenseRuntimeStatus.MISSING, null, null, false);
        }

        static ResolvedLicense invalid() {
            return new ResolvedLicense(true, LicenseRuntimeStatus.INVALID, null, null, false);
        }

        static ResolvedLicense present(
                SignedLicenseVerifier.VerifiedLicense verified,
                String status,
                String serverFingerprint,
                boolean serverBound
        ) {
            return new ResolvedLicense(true, status, verified, serverFingerprint, serverBound);
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
                        serverFingerprint,
                        serverBound,
                        null
                );
            }
            return LicenseServiceImpl.toResponse(verified, status, serverFingerprint, serverBound);
        }
    }
}
