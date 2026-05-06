package ru.pifms.server.dto.signature;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignatureFilePresignedUrlResponse {

    private UUID signatureId;
    private String originalFileName;
    private String url;
    private Instant expiresAt;
}
