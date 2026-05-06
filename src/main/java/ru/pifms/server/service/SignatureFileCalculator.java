package ru.pifms.server.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SignatureFileCalculator {

    private static final int BUFFER_SIZE = 8192;
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final int firstBytesCount;

    public SignatureFileCalculator(@Value("${malware-signatures.first-bytes-hex-length:16}") int firstBytesHexLength) {
        if (firstBytesHexLength <= 0 || firstBytesHexLength % 2 != 0) {
            throw new IllegalArgumentException("malware-signatures.first-bytes-hex-length must be a positive even number");
        }
        this.firstBytesCount = firstBytesHexLength / 2;
    }

    public CalculatedFileSignature calculate(String threatName, MultipartFile file, String requestedFileType) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        }
        if (threatName == null || threatName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "threatName is required");
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] firstBytes = inputStream.readNBytes(firstBytesCount);
            if (firstBytes.length < firstBytesCount) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "file must contain at least " + firstBytesCount + " bytes"
                );
            }

            MessageDigest remainderDigest = MessageDigest.getInstance("SHA-256");
            long remainderLength = digestRemainder(inputStream, remainderDigest);

            return new CalculatedFileSignature(
                threatName.trim(),
                HexFormat.of().formatHex(firstBytes).toUpperCase(),
                HexFormat.of().formatHex(remainderDigest.digest()).toUpperCase(),
                remainderLength,
                resolveFileType(file, requestedFileType),
                0,
                firstBytesCount - 1
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "failed to read uploaded file", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }

    private long digestRemainder(InputStream inputStream, MessageDigest digest) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long totalBytes = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
            totalBytes += read;
        }
        return totalBytes;
    }

    private String resolveFileType(MultipartFile file, String requestedFileType) {
        if (requestedFileType != null && !requestedFileType.isBlank()) {
            return requestedFileType.trim();
        }
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            return file.getContentType().trim();
        }
        return DEFAULT_CONTENT_TYPE;
    }

    public record CalculatedFileSignature(
        String threatName,
        String firstBytesHex,
        String remainderHashHex,
        long remainderLength,
        String fileType,
        long offsetStart,
        long offsetEnd
    ) {
    }
}
