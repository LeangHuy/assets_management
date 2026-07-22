package com.hunesion.assets_management.device.service;

import com.hunesion.assets_management.common.exception.ApiException;
import com.hunesion.assets_management.device.domain.DeviceStatus;
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
        return requireDevice(deviceId);
    }

    @Override
    @Transactional
    public DeviceResponse create(DeviceCreateRequest request) {
        licenseService.assertCanCreateDevice();

        DeviceResponse device = new DeviceResponse(
                null,
                request.name().trim(),
                normalizeOptional(request.ipAddress()),
                DeviceStatus.ACTIVE,
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
                    "At least one of name or ipAddress must be provided");
        }

        DeviceResponse existing = requireDevice(deviceId);

        String name = StringUtils.hasText(request.name()) ? request.name().trim() : existing.name();
        String ipAddress = request.ipAddress() != null
                ? normalizeOptional(request.ipAddress())
                : existing.ipAddress();

        DeviceResponse updated = new DeviceResponse(
                existing.id(),
                name,
                ipAddress,
                existing.status(),
                existing.createdAt(),
                existing.updatedAt()
        );

        deviceRepository.update(updated);
        return deviceRepository.findById(deviceId);
    }

    @Override
    @Transactional
    public void delete(Long deviceId) {
        DeviceResponse existing = requireDevice(deviceId);
        if (DeviceStatus.INACTIVE.equals(existing.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "Device is already inactive: " + deviceId);
        }
        deviceRepository.updateStatus(deviceId, DeviceStatus.INACTIVE);
    }

    @Override
    @Transactional
    public DeviceResponse recover(Long deviceId) {
        DeviceResponse existing = requireDevice(deviceId);
        if (DeviceStatus.ACTIVE.equals(existing.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "Device is already active: " + deviceId);
        }

//        licenseService.assertCanCreateDevice();
        deviceRepository.updateStatus(deviceId, DeviceStatus.ACTIVE);
        return deviceRepository.findById(deviceId);
    }

    private DeviceResponse requireDevice(Long deviceId) {
        DeviceResponse device = deviceRepository.findById(deviceId);
        if (device == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Device not found: " + deviceId);
        }
        return device;
    }

    private static boolean hasPatchFields(DevicePatchRequest request) {
        return StringUtils.hasText(request.name()) || request.ipAddress() != null;
    }

    private static String normalizeOptional(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
