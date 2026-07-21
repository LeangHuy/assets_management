package com.hunesion.assets_management.license.controller;

import com.hunesion.assets_management.common.dto.ApiResponse;
import com.hunesion.assets_management.license.dto.LicenseInfoResponse;
import com.hunesion.assets_management.license.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/license")
@Tag(name = "License", description = "Upload and activate signed .lic files for this server")
public class LicenseController {

    private final LicenseService licenseService;

    @GetMapping
    @Operation(
            summary = "Get license status",
            description = "Returns the active license status by re-verifying the on-disk .lic file."
    )
    public ResponseEntity<ApiResponse<LicenseInfoResponse>> status() {
        return ResponseEntity.ok(ApiResponse.ok("License status retrieved successfully", licenseService.status()));
    }

    @PostMapping(value = "/activate/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload and activate .lic file",
            description = "Uploads a signed .lic file downloaded from license-key-format, verifies the Ed25519 "
                    + "signature with the configured public key, and stores it as the active license."
    )
    public ResponseEntity<ApiResponse<LicenseInfoResponse>> activateUpload(
            @Parameter(description = "Signed .lic file", required = true)
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(
                ApiResponse.ok("License activated successfully", licenseService.activateFromFile(file)));
    }
}
