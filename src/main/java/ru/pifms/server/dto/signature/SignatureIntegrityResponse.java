package ru.pifms.server.dto.signature;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignatureIntegrityResponse {

    private UUID id;
    private boolean valid;
}
