package com.hunesion.assets_management.license.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "license.storage")
public class LicenseStorageProperties {

    /**
     * Path to the active signed license file on disk.
     */
    private String path = "data/license/active.lic";

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
