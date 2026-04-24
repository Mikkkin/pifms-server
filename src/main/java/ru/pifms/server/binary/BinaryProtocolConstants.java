package ru.pifms.server.binary;

import java.nio.charset.StandardCharsets;

public final class BinaryProtocolConstants {

    public static final int VERSION = 1;
    public static final long NO_SINCE_EPOCH_MILLIS = -1L;
    public static final byte[] MANIFEST_MAGIC = "MF-Khangildin".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] DATA_MAGIC = "DB-Khangildin".getBytes(StandardCharsets.US_ASCII);
    public static final int SHA_256_LENGTH = 32;

    private BinaryProtocolConstants() {
    }
}
