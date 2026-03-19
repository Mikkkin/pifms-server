package ru.pifms.server.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.pifms.server.entity.Device;
import ru.pifms.server.entity.DeviceLicense;
import ru.pifms.server.entity.License;

@Repository
public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, UUID> {

	long countByLicense(License license);

	boolean existsByLicenseAndDevice(License license, Device device);

	Optional<DeviceLicense> findFirstByLicenseOrderByActivationDateAsc(License license);
}
