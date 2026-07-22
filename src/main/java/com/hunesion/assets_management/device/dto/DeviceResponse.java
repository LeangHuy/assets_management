package com.hunesion.assets_management.device.dto;

import java.time.LocalDateTime;

public record DeviceResponse(
        Long id,
        String name,
        String ipAddress,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
