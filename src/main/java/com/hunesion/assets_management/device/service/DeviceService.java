package com.hunesion.assets_management.device.service;

import com.hunesion.assets_management.device.dto.DeviceCreateRequest;
import com.hunesion.assets_management.device.dto.DeviceImportRequest;
import com.hunesion.assets_management.device.dto.DeviceImportResult;
import com.hunesion.assets_management.device.dto.DevicePatchRequest;
import com.hunesion.assets_management.device.dto.DeviceResponse;

import java.util.List;

public interface DeviceService {

    List<DeviceResponse> findAll();

    DeviceResponse findById(Long deviceId);

    DeviceResponse create(DeviceCreateRequest request);

    DeviceImportResult importDevices(DeviceImportRequest request);

    DeviceResponse patch(Long deviceId, DevicePatchRequest request);

    void delete(Long deviceId);

    DeviceResponse recover(Long deviceId);
}
