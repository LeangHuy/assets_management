package com.hunesion.assets_management.device.dto;

import jakarta.validation.constraints.Size;

public record DevicePatchRequest(
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @Size(max = 45, message = "ipAddress must be at most 45 characters")
        String ipAddress
) {
}
