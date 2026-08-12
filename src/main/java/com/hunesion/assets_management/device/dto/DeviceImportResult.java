package com.hunesion.assets_management.device.dto;

import java.util.List;

public record DeviceImportResult(
        List<DeviceResponse> created,
        int createdCount,
        int skippedCount,
        List<DeviceImportErrorItem> errors
) {
}
