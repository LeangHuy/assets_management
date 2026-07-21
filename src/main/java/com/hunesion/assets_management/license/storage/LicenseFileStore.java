package com.hunesion.assets_management.license.storage;

import com.hunesion.assets_management.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Persists the canonical signed {@code .lic} wire string on disk.
 * Limits and expiry are always derived from this file after signature verification.
 */
@Component
public class LicenseFileStore {

    private final Path licensePath;

    public LicenseFileStore(LicenseStorageProperties properties) {
        this.licensePath = Path.of(properties.getPath().trim());
    }

    public boolean exists() {
        return Files.isRegularFile(licensePath);
    }

    public Path path() {
        return licensePath;
    }

    public String read() {
        if (!exists()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No active license file");
        }
        try {
            return Files.readString(licensePath, StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read license file");
        }
    }

    public void write(String licenseKey) {
        try {
            Path parent = licensePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path temp = licensePath.resolveSibling(licensePath.getFileName() + ".tmp");
            Files.writeString(temp, licenseKey, StandardCharsets.UTF_8);
            Files.move(temp, licensePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write license file");
        }
    }

    public void delete() {
        try {
            Files.deleteIfExists(licensePath);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete license file");
        }
    }
}
