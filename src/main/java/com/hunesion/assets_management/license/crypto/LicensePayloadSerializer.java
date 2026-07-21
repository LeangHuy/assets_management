package com.hunesion.assets_management.license.crypto;

import org.springframework.stereotype.Component;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

@Component
public class LicensePayloadSerializer {

    private final JsonMapper mapper;

    public LicensePayloadSerializer() {
        this.mapper = JsonMapper.builder()
                .disable(SerializationFeature.INDENT_OUTPUT)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
    }

    public byte[] serialize(LicensePayload payload) {
        try {
            return mapper.writeValueAsBytes(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize license payload", ex);
        }
    }

    public LicensePayload deserialize(byte[] bytes) {
        try {
            return mapper.readValue(bytes, LicensePayload.class);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to deserialize license payload", ex);
        }
    }
}
