package com.hunesion.assets_management.device.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DeviceImportRequest(
        @NotEmpty(message = "devices must not be empty")
        @Valid
        List<DeviceImportRow> devices
) {
}
