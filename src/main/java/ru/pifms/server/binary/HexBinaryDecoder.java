package ru.pifms.server.binary;

public final class HexBinaryDecoder {

    private HexBinaryDecoder() {
    }

    public static byte[] decode(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(fieldName + " must not be blank");
        }
        if (value.length() % 2 != 0) {
            throw new IllegalStateException(fieldName + " must contain an even number of hex characters");
        }

        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < value.length(); i += 2) {
            int high = Character.digit(value.charAt(i), 16);
            int low = Character.digit(value.charAt(i + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalStateException(fieldName + " must contain only hex characters");
            }
            bytes[i / 2] = (byte) ((high << 4) | low);
        }
        return bytes;
    }
}
