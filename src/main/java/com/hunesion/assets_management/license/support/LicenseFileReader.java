package com.hunesion.assets_management.license.support;

import com.hunesion.assets_management.common.exception.ApiException;
import com.hunesion.assets_management.license.crypto.LicenseEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class LicenseFileReader {

    public String readLicenseKey(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "License file is required");
        }

        String originalName = file.getOriginalFilename();
        if (StringUtils.hasText(originalName) && !originalName.toLowerCase().endsWith(".lic")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "License file must use the .lic extension");
        }

        String licenseKey;
        try {
            licenseKey = new String(file.getBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to read license file");
        }

        if (!StringUtils.hasText(licenseKey)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "License file is empty");
        }
        if (!licenseKey.startsWith(LicenseEncoder.PREFIX + ".")) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Invalid license file: expected " + LicenseEncoder.PREFIX + " wire format");
        }
        return licenseKey;
    }
}
