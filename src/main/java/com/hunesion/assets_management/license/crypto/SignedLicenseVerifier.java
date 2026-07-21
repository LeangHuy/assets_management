package com.hunesion.assets_management.license.crypto;

import com.hunesion.assets_management.common.exception.ApiException;
import com.hunesion.assets_management.license.signing.PublicKeyProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.security.Signature;

@Component
public class SignedLicenseVerifier {

    private final LicenseEncoder licenseEncoder;
    private final LicensePayloadSerializer payloadSerializer;
    private final PublicKeyProvider publicKeyProvider;

    public SignedLicenseVerifier(
            LicenseEncoder licenseEncoder,
            LicensePayloadSerializer payloadSerializer,
            PublicKeyProvider publicKeyProvider
    ) {
        this.licenseEncoder = licenseEncoder;
        this.payloadSerializer = payloadSerializer;
        this.publicKeyProvider = publicKeyProvider;
    }

    public VerifiedLicense verify(String licenseKey) {
        LicenseEncoder.EncodedLicense encoded = licenseEncoder.decode(licenseKey);
        if (!verifySignature(encoded.payloadBytes(), encoded.signatureBytes())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid license signature");
        }

        LicensePayload payload;
        try {
            payload = payloadSerializer.deserialize(encoded.payloadBytes());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid license payload");
        }

        return new VerifiedLicense(encoded.raw(), encoded.payloadBytes(), payload);
    }

    private boolean verifySignature(byte[] payloadBytes, byte[] signatureBytes) {
        try {
            Signature signature = Signature.getInstance("Ed25519");
            signature.initVerify(publicKeyProvider.publicKey());
            signature.update(payloadBytes);
            return signature.verify(signatureBytes);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid license signature");
        }
    }

    public record VerifiedLicense(String licenseKey, byte[] payloadBytes, LicensePayload payload) {
    }
}
