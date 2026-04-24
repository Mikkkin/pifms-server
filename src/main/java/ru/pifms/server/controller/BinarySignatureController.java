package ru.pifms.server.controller;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ru.pifms.server.dto.signature.SignatureIdsRequest;
import ru.pifms.server.service.BinarySignatureExportService;
import ru.pifms.server.service.MultipartMixedResponseFactory;

@RestController
@RequestMapping("/api/binary/signatures")
@RequiredArgsConstructor
public class BinarySignatureController {

    private final BinarySignatureExportService binarySignatureExportService;
    private final MultipartMixedResponseFactory multipartMixedResponseFactory;

    @GetMapping(value = "/full", produces = "multipart/mixed")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<MultiValueMap<String, Object>> getFullDatabase() {
        return multipartMixedResponseFactory.create(binarySignatureExportService.buildFullExport());
    }

    @GetMapping(value = "/increment", produces = "multipart/mixed")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<MultiValueMap<String, Object>> getIncrement(@RequestParam Instant since) {
        return multipartMixedResponseFactory.create(binarySignatureExportService.buildIncrementExport(since));
    }

    @PostMapping(value = "/by-ids", produces = "multipart/mixed")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<MultiValueMap<String, Object>> getByIds(@Valid @RequestBody SignatureIdsRequest request) {
        return multipartMixedResponseFactory.create(binarySignatureExportService.buildByIdsExport(request.getIds()));
    }
}
