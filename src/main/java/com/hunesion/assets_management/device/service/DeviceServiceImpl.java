package com.hunesion.assets_management.device.service;

import com.hunesion.assets_management.common.exception.ApiException;
import com.hunesion.assets_management.device.dto.DeviceCreateRequest;
import com.hunesion.assets_management.device.dto.DevicePatchRequest;
import com.hunesion.assets_management.device.dto.DeviceResponse;
import com.hunesion.assets_management.device.repository.DeviceRepository;
import com.hunesion.assets_management.license.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private final DeviceRepository deviceRepository;
    private final LicenseService licenseService;

    @Override
    @Transactional(readOnly = true)
    public List<DeviceResponse> findAll() {
        return deviceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceResponse findById(Long deviceId) {
        DeviceResponse device = deviceRepository.findById(deviceId);
        if (device == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Device not found: " + deviceId);
        }
        return device;
    }

    @Override
    @Transactional
    public DeviceResponse create(DeviceCreateRequest request) {
        licenseService.assertCanCreateDevice();

        String macAddress = normalizeMacAddress(request.macAddress());
        if (macAddress != null && deviceRepository.existsByMacAddress(macAddress)) {
            throw new ApiException(HttpStatus.CONFLICT, "MAC address already exists: " + macAddress);
        }

        DeviceResponse device = new DeviceResponse(
                null,
                request.name().trim(),
                normalizeOptional(request.ipAddress()),
                macAddress,
                resolveStatus(request.status()),
                null,
                null
        );

        deviceRepository.insert(device);
        return deviceRepository.findById(deviceRepository.lastInsertId());
    }

    @Override
    @Transactional
    public DeviceResponse patch(Long deviceId, DevicePatchRequest request) {
        if (!hasPatchFields(request)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "At least one of name, ipAddress, macAddress, or status must be provided");
        }

        DeviceResponse existing = deviceRepository.findById(deviceId);
        if (existing == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Device not found: " + deviceId);
        }

        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.name();
        String ipAddress = request.ipAddress() != null
                ? normalizeOptional(request.ipAddress())
                : existing.ipAddress();

        String macAddress = existing.macAddress();
        if (request.macAddress() != null) {
            macAddress = normalizeMacAddress(request.macAddress());
            if (macAddress != null
                    && !macAddress.equals(existing.macAddress())
                    && deviceRepository.existsByMacAddressAndIdNot(macAddress, existing.id())) {
                throw new ApiException(HttpStatus.CONFLICT, "MAC address already exists: " + macAddress);
            }
        }

        String status = StringUtils.hasText(request.status()) ? request.status().trim() : existing.status();

        DeviceResponse updated = new DeviceResponse(
                existing.id(),
                name,
                ipAddress,
                macAddress,
                status,
                existing.createdAt(),
                existing.updatedAt()
        );

        deviceRepository.update(updated);
        return deviceRepository.findById(deviceId);
    }

    private static boolean hasPatchFields(DevicePatchRequest request) {
        return StringUtils.hasText(request.name())
                || request.ipAddress() != null
                || request.macAddress() != null
                || StringUtils.hasText(request.status());
    }

    private static String resolveStatus(String status) {
        return StringUtils.hasText(status) ? status.trim() : DEFAULT_STATUS;
    }

    private static String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String normalizeMacAddress(String macAddress) {
        return StringUtils.hasText(macAddress) ? macAddress.trim() : null;
    }
}
