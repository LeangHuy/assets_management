package com.hunesion.assets_management.device.controller;

import com.hunesion.assets_management.common.dto.ApiResponse;
import com.hunesion.assets_management.device.dto.DeviceCreateRequest;
import com.hunesion.assets_management.device.dto.DevicePatchRequest;
import com.hunesion.assets_management.device.dto.DeviceResponse;
import com.hunesion.assets_management.device.service.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/devices")
@Tag(name = "Device", description = "Device CRUD operations")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    @Operation(summary = "List devices", description = "Returns all registered devices.")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok("Devices retrieved successfully", deviceService.findAll()));
    }

    @GetMapping("/{deviceId}")
    @Operation(summary = "Get device by ID", description = "Returns a single device by primary key.")
    public ResponseEntity<ApiResponse<DeviceResponse>> getById(
            @Parameter(description = "Device ID", required = true)
            @PathVariable Long deviceId) {
        return ResponseEntity.ok(ApiResponse.ok("Device retrieved successfully", deviceService.findById(deviceId)));
    }

    @PostMapping
    @Operation(
            summary = "Create device",
            description = "Creates a new device when an ACTIVE license allows it. "
                    + "Upload and activate a .lic file first if no license is present."
    )
    public ResponseEntity<ApiResponse<DeviceResponse>> create(@Valid @RequestBody DeviceCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Device created successfully", deviceService.create(request)));
    }

    @PatchMapping("/{deviceId}")
    @Operation(summary = "Update device", description = "Partially updates an existing device by ID.")
    public ResponseEntity<ApiResponse<DeviceResponse>> patch(
            @Parameter(description = "Device ID", required = true)
            @PathVariable Long deviceId,
            @Valid @RequestBody DevicePatchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Device updated successfully", deviceService.patch(deviceId, request)));
    }
}
