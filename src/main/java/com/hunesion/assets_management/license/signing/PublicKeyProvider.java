package com.hunesion.assets_management.license.signing;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class PublicKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(PublicKeyProvider.class);

    private final SigningKeyProperties properties;

    private PublicKey publicKey;

    public PublicKeyProvider(SigningKeyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        try {
            String encoded = requireText(properties.getPublicKey(), "license.signing.public-key");
            this.publicKey = decodePublicKey(encoded);
            log.info("Loaded Ed25519 public verification key");
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to load Ed25519 public verification key. Set license.signing.public-key "
                            + "(LICENSE_SIGNING_PUBLIC_KEY) from the license management system.",
                    ex
            );
        }
    }

    public PublicKey publicKey() {
        return publicKey;
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
