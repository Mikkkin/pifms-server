package ru.pifms.server.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.pifms.server.entity.LicenseType;

@Repository
public interface LicenseTypeRepository extends JpaRepository<LicenseType, UUID> {
}