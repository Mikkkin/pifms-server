package ru.pifms.server.service;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.pifms.server.binary.BinarySignaturePackage;

@Component
public class MultipartMixedResponseFactory {

    private static final MediaType MULTIPART_MIXED = MediaType.parseMediaType("multipart/mixed");

    public ResponseEntity<MultiValueMap<String, Object>> create(BinarySignaturePackage binaryPackage) {
        LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("manifest", createBinaryPart("manifest.bin", binaryPackage.manifest()));
        body.add("data", createBinaryPart("data.bin", binaryPackage.data()));

        return ResponseEntity.ok()
            .contentType(MULTIPART_MIXED)
            .body(body);
    }

    private HttpEntity<ByteArrayResource> createBinaryPart(String filename, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(content.length);

        return new HttpEntity<>(new NamedByteArrayResource(content, filename), headers);
    }

    private static class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
