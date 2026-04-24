package ru.pifms.server.binary;

public record BinarySignaturePackage(byte[] manifest, byte[] data) {

    public BinarySignaturePackage {
        if (manifest == null || data == null) {
            throw new IllegalArgumentException("binary package parts must not be null");
        }
    }
}
