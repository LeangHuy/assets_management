package com.hunesion.assets_management.license.fingerprint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Computes a server fingerprint from host attributes only (no installation id).
 *
 * <pre>
 * SHA-256(UTF-8(os_machine_id + "|" + host_identity + "|" + primary_mac))
 * </pre>
 */
@Slf4j
@Component
public class ServerFingerprintProvider {

    public static final int FINGERPRINT_VERSION = 2;

    public String compute() {
        String osMachineId = readOsMachineId();
        String hostIdentity = readHostIdentity();
        String primaryMac = readPrimaryMac();
        String canonical = String.join("|", osMachineId, hostIdentity, primaryMac);
        String fingerprint = sha256Hex(canonical);
        log.info(
                "Computed server fingerprint version={}: osMachineId={}, hostIdentity={}, primaryMac={}, fingerprint={}",
                FINGERPRINT_VERSION,
                osMachineId,
                hostIdentity,
                primaryMac.isEmpty() ? "(empty)" : primaryMac,
                fingerprint
        );
        return fingerprint;
    }

    String readOsMachineId() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            String machineGuid = readWindowsMachineGuid();
            log.info("Read Windows MachineGuid: {}", machineGuid.isEmpty() ? "(empty)" : machineGuid);
            return machineGuid;
        }
        String machineId = readLinuxMachineId();
        log.info("Read Linux machine-id: {}", machineId.isEmpty() ? "(empty)" : machineId);
        return machineId;
    }

    String readHostIdentity() {
        String hostname = nullToEmpty(readHostname());
        String systemUuid = nullToEmpty(readSystemUuid());
        if (hostname.isEmpty() && systemUuid.isEmpty()) {
            log.info("Host identity unavailable (hostname and system UUID empty)");
            return "";
        }
        if (systemUuid.isEmpty()) {
            log.info("Host identity from hostname only: {}", hostname);
            return hostname;
        }
        if (hostname.isEmpty()) {
            log.info("Host identity from system UUID only: {}", systemUuid);
            return systemUuid;
        }
        String hostIdentity = hostname + "+" + systemUuid;
        log.info("Host identity: hostname={}, systemUuid={}", hostname, systemUuid);
        return hostIdentity;
    }

    String readPrimaryMac() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                log.info("Primary MAC unavailable: no network interfaces");
                return "";
            }
            for (NetworkInterface nif : Collections.list(interfaces)) {
                if (nif.isLoopback() || nif.isVirtual() || !nif.isUp()) {
                    continue;
                }
                byte[] mac = nif.getHardwareAddress();
                if (mac == null || mac.length == 0) {
                    continue;
                }
                String formatted = formatMac(mac);
                log.info("Primary MAC from interface {}: {}", nif.getName(), formatted);
                return formatted;
            }
        } catch (Exception ex) {
            log.info("Primary MAC unavailable: {}", ex.getMessage());
        }
        log.info("Primary MAC unavailable: no suitable interface");
        return "";
    }

    private static String readLinuxMachineId() {
        Path path = Path.of("/etc/machine-id");
        if (!Files.isRegularFile(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            return "";
        }
    }

    private static String readWindowsMachineGuid() {
        try {
            Process process = new ProcessBuilder(
                    "reg", "query",
                    "HKLM\\SOFTWARE\\Microsoft\\Cryptography",
                    "/v", "MachineGuid"
            ).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("MachineGuid")) {
                        String[] parts = trimmed.split("\\s+");
                        if (parts.length >= 3) {
                            return parts[parts.length - 1].trim();
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
            // Fall through to empty.
        }
        return "";
    }

    private static String readHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            String env = System.getenv("COMPUTERNAME");
            if (env == null || env.isBlank()) {
                env = System.getenv("HOSTNAME");
            }
            return env == null ? "" : env.trim();
        }
    }

    private static String readSystemUuid() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return readWindowsSystemUuid();
        }
        Path productUuid = Path.of("/sys/class/dmi/id/product_uuid");
        if (Files.isRegularFile(productUuid)) {
            try {
                return Files.readString(productUuid, StandardCharsets.UTF_8).trim();
            } catch (IOException ignored) {
                return "";
            }
        }
        return "";
    }

    private static String readWindowsSystemUuid() {
        try {
            Process process = new ProcessBuilder(
                    "wmic", "csproduct", "get", "UUID"
            ).redirectErrorStream(true).start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || "UUID".equalsIgnoreCase(trimmed)) {
                        continue;
                    }
                    return trimmed;
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
            // Fall through to empty.
        }
        return "";
    }

    private static String formatMac(byte[] mac) {
        StringBuilder sb = new StringBuilder(mac.length * 3);
        for (int i = 0; i < mac.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(String.format(Locale.ROOT, "%02x", mac[i]));
        }
        return sb.toString();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
