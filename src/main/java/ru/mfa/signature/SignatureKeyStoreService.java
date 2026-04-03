package ru.mfa.signature;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignatureKeyStoreService {

    private final SignatureProperties properties;
    private final ResourceLoader resourceLoader;

    private volatile PrivateKey privateKey;
    private volatile PublicKey publicKey;

    public PrivateKey getPrivateKey() {
        PrivateKey cached = privateKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (privateKey == null) {
                privateKey = loadPrivateKey();
            }
            return privateKey;
        }
    }

    public PublicKey getPublicKey() {
        PublicKey cached = publicKey;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (publicKey == null) {
                publicKey = loadPublicKey();
            }
            return publicKey;
        }
    }

    private PrivateKey loadPrivateKey() { 
        KeyStore keyStore = loadKeyStore();
        String alias = requireNonBlank(properties.getKeyAlias(), "signature.keyAlias is not configured");
        char[] keyPassword = resolveKeyPassword();
        try {
            java.security.Key key = keyStore.getKey(alias, keyPassword);
            if (key == null) {
                throw new IllegalStateException("Key with alias '" + alias + "' was not found in keystore");
            }
            if (!(key instanceof PrivateKey privateKeyValue)) {
                throw new IllegalStateException("Alias '" + alias + "' does not contain a private key entry");
            }
            return privateKeyValue;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load private key", ex);
        }
    }

    private PublicKey loadPublicKey() {
        KeyStore keyStore = loadKeyStore();
        String alias = requireNonBlank(properties.getKeyAlias(), "signature.keyAlias is not configured");
        try {
            Certificate certificate = keyStore.getCertificate(alias);
            if (certificate == null) {
                throw new IllegalStateException("Certificate for alias '" + alias + "' was not found in keystore");
            }
            return certificate.getPublicKey();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load public key", ex);
        }
    }

    private KeyStore loadKeyStore() {
        String keyStorePath = requireNonBlank(properties.getKeyStorePath(), "signature.keyStorePath is not configured");
        String keyStoreType = properties.getKeyStoreType() == null || properties.getKeyStoreType().isBlank()
                ? "JKS"
                : properties.getKeyStoreType();
        char[] keyStorePassword = requireNonBlank(
                properties.getKeyStorePassword(),
                "signature.keyStorePassword is not configured"
        ).toCharArray();

        try (InputStream inputStream = openKeyStoreStream(keyStorePath)) {
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(inputStream, keyStorePassword);
            return keyStore;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load keystore from path: " + keyStorePath, ex);
        }
    }

    private char[] resolveKeyPassword() {
        String keyPassword = properties.getKeyPassword();
        if (keyPassword != null && !keyPassword.isBlank()) {
            return keyPassword.toCharArray();
        }
        return requireNonBlank(properties.getKeyStorePassword(), "signature.keyStorePassword is not configured")
                .toCharArray();
    }

    private InputStream openKeyStoreStream(String keyStorePath) throws Exception {
        String normalizedPath = keyStorePath.trim();
        String lowerPath = normalizedPath.toLowerCase();
        if (lowerPath.startsWith("classpath:") || lowerPath.startsWith("file:")) {
            Resource resource = resourceLoader.getResource(normalizedPath);
            if (!resource.exists()) {
                throw new IllegalStateException("Keystore resource was not found: " + normalizedPath);
            }
            return resource.getInputStream();
        }
        return Files.newInputStream(Path.of(normalizedPath));
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }
}
