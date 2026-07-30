package com.hunesion.assets_management.license.service;

import com.hunesion.assets_management.license.dto.LicenseInfoResponse;
import org.springframework.web.multipart.MultipartFile;

public interface LicenseService {

    LicenseInfoResponse activateFromFile(MultipartFile file);

    LicenseInfoResponse status();

    void assertCanCreateDevice();

    /**
     * Builds {@code official-license-request.json} from the active bound demo license
     * and returns the file bytes for download.
     */
    byte[] generateOfficialLicenseRequest();
}
