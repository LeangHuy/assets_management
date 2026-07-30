package com.hunesion.assets_management.license.fingerprint;

import com.hunesion.license.runtime.exception.LicenseException;
import com.hunesion.license.runtime.storage.LicenseStorageProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Persists {@code fingerprint.meta} in the license storage directory.
 */
@Slf4j
@Component
public class LicenseBindingStore {

    public static final String FILE_NAME = "fingerprint.meta";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final Path metaPath;

    public LicenseBindingStore(LicenseStorageProperties storageProperties) {
        Path licensePath = Path.of(storageProperties.getPath().trim());
        Path parent = licensePath.getParent();
        this.metaPath = (parent != null ? parent : Path.of(".")).resolve(FILE_NAME);
        log.info("License binding store path={}", metaPath.toAbsolutePath());
    }

    public Optional<FingerprintMeta> read() {
        if (!Files.isRegularFile(metaPath)) {
            log.info("fingerprint.meta not found at {}", metaPath.toAbsolutePath());
            return Optional.empty();
        }
        try {
            FingerprintMeta meta = MAPPER.readValue(metaPath.toFile(), FingerprintMeta.class);
            log.info(
                    "Read fingerprint.meta: fingerprint={}, version={}, computedAt={}, payloadHash={}",
                    meta.serverFingerprint(),
                    meta.fingerprintVersion(),
                    meta.computedAt(),
                    meta.payloadHash()
            );
            return Optional.of(meta);
        } catch (Exception ex) {
            throw new LicenseException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read fingerprint.meta");
        }
    }

    public void write(FingerprintMeta meta) {
        try {
            Path parent = metaPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = metaPath.resolveSibling(FILE_NAME + ".tmp");
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), meta);
            Files.move(temp, metaPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.info(
                    "Wrote fingerprint.meta to {}: fingerprint={}, version={}, computedAt={}, payloadHash={}",
                    metaPath.toAbsolutePath(),
                    meta.serverFingerprint(),
                    meta.fingerprintVersion(),
                    meta.computedAt(),
                    meta.payloadHash()
            );
        } catch (IOException ex) {
            throw new LicenseException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write fingerprint.meta");
        }
    }
}
