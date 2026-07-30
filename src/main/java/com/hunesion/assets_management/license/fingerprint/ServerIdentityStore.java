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
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists {@code installation.meta} in the license storage directory.
 */
@Slf4j
@Component
public class ServerIdentityStore {

    public static final String FILE_NAME = "installation.meta";
    public static final int FINGERPRINT_VERSION = 1;

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final Path metaPath;
    private final Clock clock;

    public ServerIdentityStore(LicenseStorageProperties storageProperties, Clock clock) {
        Path licensePath = Path.of(storageProperties.getPath().trim());
        Path parent = licensePath.getParent();
        this.metaPath = (parent != null ? parent : Path.of(".")).resolve(FILE_NAME);
        this.clock = clock;
        log.info("Server identity store path={}", metaPath.toAbsolutePath());
    }

    public Optional<InstallationMeta> read() {
        if (!Files.isRegularFile(metaPath)) {
            log.info("installation.meta not found at {}", metaPath.toAbsolutePath());
            return Optional.empty();
        }
        try {
            InstallationMeta meta = MAPPER.readValue(metaPath.toFile(), InstallationMeta.class);
            log.info(
                    "Read installation.meta: installationId={}, fingerprintVersion={}, createdAt={}",
                    meta.installationId(),
                    meta.fingerprintVersion(),
                    meta.createdAt()
            );
            return Optional.of(meta);
        } catch (Exception ex) {
            throw new LicenseException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read installation.meta");
        }
    }

    /**
     * Returns the existing installation id, or creates and persists a new one.
     */
    public InstallationMeta ensurePresent() {
        Optional<InstallationMeta> existing = read();
        if (existing.isPresent()) {
            log.info("Using existing installationId={}", existing.get().installationId());
            return existing.get();
        }
        InstallationMeta created = new InstallationMeta(
                UUID.randomUUID().toString(),
                FINGERPRINT_VERSION,
                Instant.now(clock)
        );
        write(created);
        log.info("Created new installationId={}", created.installationId());
        return created;
    }

    public void write(InstallationMeta meta) {
        try {
            Path parent = metaPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = metaPath.resolveSibling(FILE_NAME + ".tmp");
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), meta);
            Files.move(temp, metaPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.info(
                    "Wrote installation.meta to {}: installationId={}, fingerprintVersion={}, createdAt={}",
                    metaPath.toAbsolutePath(),
                    meta.installationId(),
                    meta.fingerprintVersion(),
                    meta.createdAt()
            );
        } catch (IOException ex) {
            throw new LicenseException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write installation.meta");
        }
    }
}
