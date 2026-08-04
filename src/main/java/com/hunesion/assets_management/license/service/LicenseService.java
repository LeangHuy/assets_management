package com.hunesion.assets_management.license.service;

import com.hunesion.assets_management.license.dto.LicenseInfoResponse;
import org.springframework.web.multipart.MultipartFile;

public interface LicenseService {

    LicenseInfoResponse activateFromFile(MultipartFile file);

    LicenseInfoResponse status();

    void assertCanCreateDevice();

    /**
     * Builds an offline license-request JSON from the active bound TEMPORARY (CONVERSION)
     * or OFFICIAL (RENEWAL) license and returns the file bytes plus download filename.
     */
    LicenseRequestFile generateOfficialLicenseRequest();

    record LicenseRequestFile(byte[] content, String filename) {
    }
}
