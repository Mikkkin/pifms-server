package ru.pifms.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class SignatureFileCalculatorTest {

    private final SignatureFileCalculator calculator = new SignatureFileCalculator(16);

    @Test
    void calculateBuildsSignatureFieldsFromFileContent() throws Exception {
        byte[] content = new byte[] {
            0x01, 0x02, 0x03, 0x04,
            0x05, 0x06, 0x07, 0x08,
            0x09, 0x0A
        };
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "sample.bin",
            "application/octet-stream",
            content
        );

        SignatureFileCalculator.CalculatedFileSignature signature =
            calculator.calculate(" Test threat ", file, null);

        assertEquals("Test threat", signature.threatName());
        assertEquals("0102030405060708", signature.firstBytesHex());
        assertEquals(2, signature.remainderLength());
        assertEquals(sha256Hex(new byte[] {0x09, 0x0A}), signature.remainderHashHex());
        assertEquals("application/octet-stream", signature.fileType());
        assertEquals(0, signature.offsetStart());
        assertEquals(7, signature.offsetEnd());
    }

    @Test
    void calculateRejectsTooSmallFile() {
        MockMultipartFile file = new MockMultipartFile("file", "small.bin", null, new byte[] {0x01, 0x02});

        assertThrows(ResponseStatusException.class, () -> calculator.calculate("Threat", file, null));
    }

    private String sha256Hex(byte[] value) throws Exception {
        return HexFormat.of()
            .formatHex(MessageDigest.getInstance("SHA-256").digest(value))
            .toUpperCase();
    }
}
