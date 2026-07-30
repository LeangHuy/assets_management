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
 * Persists {@code official-license-request.json} in the license storage directory.
 */
@Slf4j
@Component
public class OfficialLicenseRequestStore {

    public static final String FILE_NAME = "official-license-request.json";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final Path requestPath;

    public OfficialLicenseRequestStore(LicenseStorageProperties storageProperties) {
        Path licensePath = Path.of(storageProperties.getPath().trim());
        Path parent = licensePath.getParent();
        this.requestPath = (parent != null ? parent : Path.of(".")).resolve(FILE_NAME);
        log.info("Official license request store path={}", requestPath.toAbsolutePath());
    }

    public byte[] write(OfficialLicenseRequest request) {
        try {
            Path parent = requestPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            Path temp = requestPath.resolveSibling(FILE_NAME + ".tmp");
            Files.write(temp, bytes);
            Files.move(temp, requestPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.info(
                    "Wrote {} to {}: requestId={}, demoLicenseId={}, installationId={}, serverFingerprint={}",
                    FILE_NAME,
                    requestPath.toAbsolutePath(),
                    request.requestId(),
                    request.demoLicense().licenseId(),
                    request.installation().installationId(),
                    request.installation().serverFingerprint()
            );
            return bytes;
        } catch (IOException ex) {
            throw new LicenseException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write " + FILE_NAME);
        }
    }
}
