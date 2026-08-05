package com.hunesion.assets_management.license.fingerprint;

import com.hunesion.assets_management.license.dto.OfficialLicenseRequest;
import com.hunesion.license.runtime.exception.LicenseException;
import com.hunesion.license.runtime.storage.LicenseStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Persists offline license-request JSON beside the license storage directory.
 */
@Slf4j
@Component
public class OfficialLicenseRequestStore {

    public static final String CONVERSION_FILE_NAME = "official-license-request.json";
    public static final String RENEWAL_FILE_NAME = "renewal-official-license-request.json";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final Path storageDir;

    public OfficialLicenseRequestStore(LicenseStorageProperties storageProperties) {
        Path licensePath = Path.of(storageProperties.getPath().trim());
        Path parent = licensePath.getParent();
        this.storageDir = parent != null ? parent : Path.of(".");
        log.info("Official license request store dir={}", storageDir.toAbsolutePath());
    }

    public static String fileNameFor(OfficialLicenseRequest request) {
        if (OfficialLicenseRequest.REQUEST_TYPE_RENEWAL.equalsIgnoreCase(request.requestType())) {
            return RENEWAL_FILE_NAME;
        }
        return CONVERSION_FILE_NAME;
    }

    public byte[] write(OfficialLicenseRequest request) {
        String fileName = fileNameFor(request);
        Path requestPath = storageDir.resolve(fileName);
        try {
            Files.createDirectories(storageDir);
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            Path temp = requestPath.resolveSibling(fileName + ".tmp");
            Files.write(temp, bytes);
            Files.move(temp, requestPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.info(
                    "Wrote {} to {}: requestId={}, requestType={}, sourceLicenseId={}, serverFingerprint={}",
                    fileName,
                    requestPath.toAbsolutePath(),
                    request.requestId(),
                    request.requestType(),
                    request.demoLicense().licenseId(),
                    request.installation().serverFingerprint()
            );
            return bytes;
        } catch (IOException ex) {
            throw new LicenseException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write " + fileName);
        }
    }
}
