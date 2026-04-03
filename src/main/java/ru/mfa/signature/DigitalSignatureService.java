package ru.mfa.signature;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class DigitalSignatureService {

    private final SignatureProperties properties;
    private final JsonCanonicalizer jsonCanonicalizer;
    private final SignatureKeyStoreService signatureKeyStoreService;

    public String sign(Object payload) {
        byte[] canonicalBytes = canonicalize(payload);

        try {
            Signature signature = Signature.getInstance(resolveAlgorithm());
            signature.initSign(signatureKeyStoreService.getPrivateKey());
            signature.update(canonicalBytes);
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to sign payload", ex);
        }
    }

    public byte[] canonicalize(Object payload) {
        return jsonCanonicalizer.canonizeJson(payload).getBytes(StandardCharsets.UTF_8);
    }

    public PublicKey getPublicKey() {
        return signatureKeyStoreService.getPublicKey();
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(getPublicKey().getEncoded());
    }

    public String getAlgorithm() {
        return resolveAlgorithm();
    }

    private String resolveAlgorithm() {
        String algorithm = properties.getAlgorithm();
        if (algorithm == null || algorithm.isBlank()) {
            return "SHA256withRSA";
        }
        return algorithm;
    }
}
