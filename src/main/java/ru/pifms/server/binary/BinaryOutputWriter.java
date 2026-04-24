package ru.pifms.server.binary;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BinaryOutputWriter {

    private static final long UINT_32_MAX = 0xFFFF_FFFFL;

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    public void writeUInt8(int value) {
        requireUnsigned(value, 0xFF, "uint8");
        output.write(value);
    }

    public void writeUInt16(int value) {
        requireUnsigned(value, 0xFFFF, "uint16");
        output.write((value >>> 8) & 0xFF);
        output.write(value & 0xFF);
    }

    public void writeUInt32(long value) {
        requireUnsigned(value, UINT_32_MAX, "uint32");
        output.write((int) ((value >>> 24) & 0xFF));
        output.write((int) ((value >>> 16) & 0xFF));
        output.write((int) ((value >>> 8) & 0xFF));
        output.write((int) (value & 0xFF));
    }

    public void writeInt64(long value) {
        output.write((int) ((value >>> 56) & 0xFF));
        output.write((int) ((value >>> 48) & 0xFF));
        output.write((int) ((value >>> 40) & 0xFF));
        output.write((int) ((value >>> 32) & 0xFF));
        output.write((int) ((value >>> 24) & 0xFF));
        output.write((int) ((value >>> 16) & 0xFF));
        output.write((int) ((value >>> 8) & 0xFF));
        output.write((int) (value & 0xFF));
    }

    public void writeUuid(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("uuid must not be null");
        }
        writeInt64(value.getMostSignificantBits());
        writeInt64(value.getLeastSignificantBits());
    }

    public void writeUtf8(String value) {
        if (value == null) {
            throw new IllegalArgumentException("string must not be null");
        }
        writeByteArray(value.getBytes(StandardCharsets.UTF_8));
    }

    public void writeByteArray(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("byte array must not be null");
        }
        writeUInt32(value.length);
        writeRawBytes(value);
    }

    public void writeRawBytes(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("byte array must not be null");
        }
        output.writeBytes(value);
    }

    public int size() {
        return output.size();
    }

    public byte[] toByteArray() {
        return output.toByteArray();
    }

    private void requireUnsigned(long value, long maxValue, String typeName) {
        if (value < 0 || value > maxValue) {
            throw new IllegalArgumentException(typeName + " value is out of range: " + value);
        }
    }
}
