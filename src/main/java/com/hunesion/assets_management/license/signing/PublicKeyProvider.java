package com.hunesion.assets_management.license.signing;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class PublicKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(PublicKeyProvider.class);

    private final SigningKeyProperties properties;

    private PublicKey publicKey;
    private String keyId;

    public PublicKeyProvider(SigningKeyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.keyId = requireText(properties.getKeyId(), "license.signing.key-id");
        try {
            String encoded = loadEncodedPublicKey();
            this.publicKey = decodePublicKey(encoded);
            log.info("Loaded Ed25519 public verification key (keyId={})", keyId);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load Ed25519 public verification key. Provide license.signing.public-key-path "
                            + "or license.signing.public-key.",
                    ex
            );
        }
    }

    public String keyId() {
        return keyId;
    }

    public PublicKey publicKey() {
        return publicKey;
    }

    private String loadEncodedPublicKey() throws Exception {
        if (StringUtils.hasText(properties.getPublicKeyPath())) {
            Path path = Path.of(properties.getPublicKeyPath().trim());
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Public key file not found: " + path.toAbsolutePath());
            }
            return Files.readString(path).trim();
        }
        if (StringUtils.hasText(properties.getPublicKey())) {
            return properties.getPublicKey().trim();
        }
        throw new IllegalStateException(
                "Ed25519 public verification key is missing. Set license.signing.public-key-path "
                        + "or license.signing.public-key."
        );
    }

    private static PublicKey decodePublicKey(String value) throws Exception {
        byte[] encoded = Base64.getDecoder().decode(stripPem(value));
        return KeyFactory.getInstance("Ed25519").generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static String stripPem(String value) {
        return value
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    private static String requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(name + " must be configured");
        }
        return value.trim();
    }
}
