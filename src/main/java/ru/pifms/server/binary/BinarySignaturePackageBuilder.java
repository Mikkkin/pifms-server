package ru.pifms.server.binary;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ru.mfa.signature.DigitalSignatureService;
import ru.pifms.server.entity.MalwareSignature;
import ru.pifms.server.entity.SignatureStatus;

@Component
@RequiredArgsConstructor
public class BinarySignaturePackageBuilder {

    private final DigitalSignatureService digitalSignatureService;

    public BinarySignaturePackage build(
        List<MalwareSignature> signatures,
        BinaryExportType exportType,
        Instant generatedAt,
        Instant since
    ) {
        Objects.requireNonNull(signatures, "signatures must not be null");
        Objects.requireNonNull(exportType, "exportType must not be null");
        Objects.requireNonNull(generatedAt, "generatedAt must not be null");

        DataBuildResult dataBuildResult = buildData(signatures);
        byte[] manifest = buildManifest(
            signatures,
            dataBuildResult.records(),
            exportType,
            generatedAt,
            since,
            sha256(dataBuildResult.data())
        );
        return new BinarySignaturePackage(manifest, dataBuildResult.data());
    }

    private DataBuildResult buildData(List<MalwareSignature> signatures) {
        BinaryOutputWriter payloadWriter = new BinaryOutputWriter();
        List<DataRecordLocation> records = new ArrayList<>(signatures.size());

        long payloadOffset = 0L;
        for (MalwareSignature signature : signatures) {
            byte[] recordBytes = serializeDataRecord(signature);
            payloadWriter.writeRawBytes(recordBytes);
            records.add(new DataRecordLocation(payloadOffset, recordBytes.length));
            payloadOffset += recordBytes.length;
        }

        BinaryOutputWriter dataWriter = new BinaryOutputWriter();
        dataWriter.writeRawBytes(BinaryProtocolConstants.DATA_MAGIC);
        dataWriter.writeUInt16(BinaryProtocolConstants.VERSION);
        dataWriter.writeUInt32(signatures.size());
        dataWriter.writeRawBytes(payloadWriter.toByteArray());

        return new DataBuildResult(dataWriter.toByteArray(), records);
    }

    private byte[] serializeDataRecord(MalwareSignature signature) {
        validateSignature(signature);

        BinaryOutputWriter writer = new BinaryOutputWriter();
        writer.writeUtf8(signature.getThreatName());
        writer.writeByteArray(HexBinaryDecoder.decode(signature.getFirstBytesHex(), "firstBytesHex"));
        writer.writeByteArray(HexBinaryDecoder.decode(signature.getRemainderHashHex(), "remainderHashHex"));
        writer.writeInt64(signature.getRemainderLength());
        writer.writeUtf8(signature.getFileType());
        writer.writeInt64(signature.getOffsetStart());
        writer.writeInt64(signature.getOffsetEnd());
        return writer.toByteArray();
    }

    private byte[] buildManifest(
        List<MalwareSignature> signatures,
        List<DataRecordLocation> records,
        BinaryExportType exportType,
        Instant generatedAt,
        Instant since,
        byte[] dataSha256
    ) {
        BinaryOutputWriter unsignedManifestWriter = new BinaryOutputWriter();
        unsignedManifestWriter.writeRawBytes(BinaryProtocolConstants.MANIFEST_MAGIC);
        unsignedManifestWriter.writeUInt16(BinaryProtocolConstants.VERSION);
        unsignedManifestWriter.writeUInt8(exportType.getCode());
        unsignedManifestWriter.writeInt64(generatedAt.toEpochMilli());
        unsignedManifestWriter.writeInt64(resolveSinceEpochMillis(exportType, since));
        unsignedManifestWriter.writeUInt32(signatures.size());
        unsignedManifestWriter.writeRawBytes(dataSha256);

        for (int i = 0; i < signatures.size(); i++) {
            writeManifestEntry(unsignedManifestWriter, signatures.get(i), records.get(i));
        }

        byte[] unsignedManifest = unsignedManifestWriter.toByteArray();
        byte[] manifestSignature = digitalSignatureService.signBytes(unsignedManifest);

        BinaryOutputWriter signedManifestWriter = new BinaryOutputWriter();
        signedManifestWriter.writeRawBytes(unsignedManifest);
        signedManifestWriter.writeByteArray(manifestSignature);
        return signedManifestWriter.toByteArray();
    }

    private void writeManifestEntry(
        BinaryOutputWriter writer,
        MalwareSignature signature,
        DataRecordLocation recordLocation
    ) {
        byte[] recordSignature = decodeRecordSignature(signature);

        writer.writeUuid(signature.getId());
        writer.writeUInt8(statusCode(signature.getStatus()));
        writer.writeInt64(signature.getUpdatedAt().toEpochMilli());
        writer.writeInt64(recordLocation.offset());
        writer.writeUInt32(recordLocation.length());
        writer.writeByteArray(recordSignature);
    }

    private long resolveSinceEpochMillis(BinaryExportType exportType, Instant since) {
        if (exportType == BinaryExportType.INCREMENT) {
            if (since == null) {
                throw new IllegalArgumentException("since must be provided for increment export");
            }
            return since.toEpochMilli();
        }
        return BinaryProtocolConstants.NO_SINCE_EPOCH_MILLIS;
    }

    private int statusCode(SignatureStatus status) {
        return switch (status) {
            case ACTUAL -> 1;
            case DELETED -> 2;
        };
    }

    private byte[] decodeRecordSignature(MalwareSignature signature) {
        try {
            return Base64.getDecoder().decode(signature.getDigitalSignatureBase64());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Stored signature has invalid Base64 for id " + signature.getId(), ex);
        }
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is not available", ex);
        }
    }

    private void validateSignature(MalwareSignature signature) {
        if (signature == null) {
            throw new IllegalArgumentException("signature must not be null");
        }
        if (signature.getId() == null) {
            throw new IllegalStateException("signature id must not be null");
        }
        if (signature.getUpdatedAt() == null) {
            throw new IllegalStateException("signature updatedAt must not be null for id " + signature.getId());
        }
        if (signature.getStatus() == null) {
            throw new IllegalStateException("signature status must not be null for id " + signature.getId());
        }
        if (signature.getDigitalSignatureBase64() == null || signature.getDigitalSignatureBase64().isBlank()) {
            throw new IllegalStateException("signature digitalSignatureBase64 must not be blank for id " + signature.getId());
        }
        if (signature.getOffsetEnd() < signature.getOffsetStart()) {
            throw new IllegalStateException("signature offsetEnd must be greater than or equal to offsetStart for id " + signature.getId());
        }
    }

    private record DataBuildResult(byte[] data, List<DataRecordLocation> records) {
    }

    private record DataRecordLocation(long offset, long length) {
    }
}
