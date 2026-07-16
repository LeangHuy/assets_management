package com.hunesion.assets_management.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest(
        @NotBlank(message = "name is required")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @Size(max = 45, message = "ipAddress must be at most 45 characters")
        String ipAddress,

        @Size(max = 17, message = "macAddress must be at most 17 characters")
        String macAddress,

        @Size(max = 50, message = "status must be at most 50 characters")
        String status
) {
}
