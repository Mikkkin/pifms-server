package ru.pifms.server.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ru.pifms.server.binary.BinaryExportType;
import ru.pifms.server.binary.BinarySignaturePackage;
import ru.pifms.server.binary.BinarySignaturePackageBuilder;

@Service
@RequiredArgsConstructor
public class BinarySignatureExportService {

    private final MalwareSignatureService malwareSignatureService;
    private final BinarySignaturePackageBuilder packageBuilder;

    public BinarySignaturePackage buildFullExport() {
        return packageBuilder.build(
            malwareSignatureService.getActualSignatures(),
            BinaryExportType.FULL,
            Instant.now(),
            null
        );
    }

    public BinarySignaturePackage buildIncrementExport(Instant since) {
        return packageBuilder.build(
            malwareSignatureService.getIncrement(since),
            BinaryExportType.INCREMENT,
            Instant.now(),
            since
        );
    }

    public BinarySignaturePackage buildByIdsExport(List<UUID> ids) {
        return packageBuilder.build(
            malwareSignatureService.getByIds(ids),
            BinaryExportType.BY_IDS,
            Instant.now(),
            null
        );
    }
}
