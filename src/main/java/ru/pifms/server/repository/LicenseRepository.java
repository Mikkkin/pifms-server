package ru.pifms.server.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ru.pifms.server.entity.Device;
import ru.pifms.server.entity.License;

@Repository
public interface LicenseRepository extends JpaRepository<License, UUID> {

    Optional<License> findByCode(String code);

    boolean existsByCode(String code);

    @Query("""
        select distinct license from License license
        join DeviceLicense deviceLicense on deviceLicense.license = license
        where deviceLicense.device = :device
          and deviceLicense.device.user.id = :userId
          and license.user.id = :userId
          and license.product.id = :productId
          and license.blocked = false
          and license.endingDate >= :now
        """)
    Optional<License> findActiveByDeviceUserAndProduct(
        @Param("device") Device device,
        @Param("userId") Long userId,
        @Param("productId") UUID productId,
        @Param("now") Instant now
    );
}