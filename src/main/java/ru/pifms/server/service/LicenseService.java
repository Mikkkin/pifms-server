package ru.pifms.server.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ru.mfa.signature.DigitalSignatureService;
import ru.pifms.server.dto.ActivateLicenseRequestDTO;
import ru.pifms.server.dto.CheckLicenseRequestDTO;
import ru.pifms.server.dto.CreateLicenseRequestDTO;
import ru.pifms.server.dto.LicenseResponseDTO;
import ru.pifms.server.dto.RenewLicenseRequestDTO;
import ru.pifms.server.dto.TicketDTO;
import ru.pifms.server.dto.TicketResponse;
import ru.pifms.server.entity.Device;
import ru.pifms.server.entity.DeviceLicense;
import ru.pifms.server.entity.License;
import ru.pifms.server.entity.LicenseHistory;
import ru.pifms.server.entity.LicenseHistoryStatus;
import ru.pifms.server.entity.LicenseType;
import ru.pifms.server.entity.Product;
import ru.pifms.server.entity.User;
import ru.pifms.server.repository.DeviceLicenseRepository;
import ru.pifms.server.repository.DeviceRepository;
import ru.pifms.server.repository.LicenseHistoryRepository;
import ru.pifms.server.repository.LicenseRepository;
import ru.pifms.server.repository.LicenseTypeRepository;
import ru.pifms.server.repository.ProductRepository;

@Service
public class LicenseService {

	private final LicenseRepository licenseRepository;
	private final DeviceRepository deviceRepository;
	private final DeviceLicenseRepository deviceLicenseRepository;
	private final LicenseHistoryRepository licenseHistoryRepository;
	private final ProductRepository productRepository;
	private final LicenseTypeRepository licenseTypeRepository;
	private final ApplicationUserService applicationUserService;
	private final DigitalSignatureService digitalSignatureService;
	private final long ticketLifetimeSeconds;

	public LicenseService(
		LicenseRepository licenseRepository,
		DeviceRepository deviceRepository,
		DeviceLicenseRepository deviceLicenseRepository,
		LicenseHistoryRepository licenseHistoryRepository,
		ProductRepository productRepository,
		LicenseTypeRepository licenseTypeRepository,
		ApplicationUserService applicationUserService,
		DigitalSignatureService digitalSignatureService,
		@Value("${license.ticket.ttl-seconds:300}") long ticketLifetimeSeconds
	) {
		this.licenseRepository = licenseRepository;
		this.deviceRepository = deviceRepository;
		this.deviceLicenseRepository = deviceLicenseRepository;
		this.licenseHistoryRepository = licenseHistoryRepository;
		this.productRepository = productRepository;
		this.licenseTypeRepository = licenseTypeRepository;
		this.applicationUserService = applicationUserService;
		this.digitalSignatureService = digitalSignatureService;
		this.ticketLifetimeSeconds = ticketLifetimeSeconds;
	}

	@Transactional
	public LicenseResponseDTO createLicense(CreateLicenseRequestDTO request, Long adminId) {
		Product product = productRepository.findById(request.getProductId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found"));
		LicenseType licenseType = licenseTypeRepository.findById(request.getTypeId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "type not found"));
		User owner = applicationUserService.getActiveUserOrFail(request.getOwnerId());
		User admin = applicationUserService.getUserOrFail(adminId);

		License license = License.builder()
			.code(generateCode())
			.product(product)
			.type(licenseType)
			.owner(owner)
			.blocked(false)
			.deviceCount(request.getDeviceCount())
			.description(request.getDescription())
			.build();

		License savedLicense = licenseRepository.save(license);
		saveHistory(savedLicense, admin, LicenseHistoryStatus.CREATED, "License created");
		return toResponse(savedLicense);
	}

	@Transactional
	public TicketResponse activateLicense(ActivateLicenseRequestDTO request, Long userId) {
		License license = findByCodeOrFail(request.getActivationKey());
		if (license.getUser() != null && !license.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "license owned by another user");
		}

		User currentUser = applicationUserService.getUserOrFail(userId);
		String normalizedDeviceMac = normalizeMacAddress(request.getDeviceMac());
		Device device = findDeviceByMacAddress(normalizedDeviceMac)
			.map(existingDevice -> validateDeviceOwner(existingDevice, userId))
			.orElseGet(() -> deviceRepository.save(Device.builder()
				.name(request.getDeviceName())
				.macAddress(normalizedDeviceMac)
				.user(currentUser)
				.build()));

		Instant now = Instant.now();
		if (license.getUser() == null) {
			license.setUser(currentUser);
			license.setFirstActivationDate(now);
			license.setEndingDate(now.plus(license.getType().getDefaultDurationInDays(), ChronoUnit.DAYS));
			licenseRepository.save(license);
			createDeviceLicenseIfMissing(license, device, now);
			saveHistory(license, currentUser, LicenseHistoryStatus.ACTIVATED, "First activation");
			return buildTicketResponse(license, device, now);
		}

		if (deviceLicenseRepository.existsByLicenseAndDevice(license, device)) {
			return buildTicketResponse(license, device, now);
		} // Превышение лимита устройств на одну лицензию

		long activeDeviceCount = deviceLicenseRepository.countByLicense(license);
		if (activeDeviceCount >= license.getDeviceCount()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "device limit reached");
		}

		createDeviceLicenseIfMissing(license, device, now);
		saveHistory(license, currentUser, LicenseHistoryStatus.ACTIVATED, "Activation on additional device");
		return buildTicketResponse(license, device, now);
	}

	@Transactional(readOnly = true)
	public TicketResponse checkLicense(CheckLicenseRequestDTO request, Long userId) {
		Device device = findDeviceByMacAddress(normalizeMacAddress(request.getDeviceMac()))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "device not found"));

		License license = licenseRepository.findActiveByDeviceUserAndProduct(device, userId, request.getProductId(), Instant.now())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "license not found"));

		return buildTicketResponse(license, device, Instant.now());
	}

	@Transactional
	public TicketResponse renewLicense(RenewLicenseRequestDTO request, Long userId) {
		License license = findByCodeOrFail(request.getActivationKey());
		if (!isRenewable(license, Instant.now())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "renewal not allowed");
		}

		User user = applicationUserService.getUserOrFail(userId);
		Instant renewedAt = Instant.now();
		Instant baseEndingDate = license.getEndingDate() == null || license.getEndingDate().isBefore(renewedAt)
			? renewedAt
			: license.getEndingDate();
		license.setEndingDate(baseEndingDate.plus(license.getType().getDefaultDurationInDays(), ChronoUnit.DAYS));
		licenseRepository.save(license);
		saveHistory(license, user, LicenseHistoryStatus.RENEWED, "License renewed");

		Device device = resolveTicketDevice(license, request.getDeviceMac());
		return buildTicketResponse(license, device, renewedAt);
	}

	private Device validateDeviceOwner(Device device, Long userId) {
		if (!device.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "device owned by another user");
		}
		return device;
	}

	private License findByCodeOrFail(String activationKey) {
		return licenseRepository.findByCode(activationKey)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "license not found"));
	}

	private void createDeviceLicenseIfMissing(License license, Device device, Instant activationDate) {
		if (deviceLicenseRepository.existsByLicenseAndDevice(license, device)) {
			return;
		}

		deviceLicenseRepository.save(DeviceLicense.builder()
			.license(license)
			.device(device)
			.activationDate(activationDate)
			.build());
	}

	private void saveHistory(License license, User user, LicenseHistoryStatus status, String description) {
		licenseHistoryRepository.save(LicenseHistory.builder()
			.license(license)
			.user(user)
			.status(status)
			.changeDate(Instant.now())
			.description(description)
			.build());
	}

	private boolean isRenewable(License license, Instant now) {
		if (license.getFirstActivationDate() == null || license.getEndingDate() == null) {
			return true;
		}

		Instant renewalWindowBoundary = now.plus(7, ChronoUnit.DAYS);
		return !license.getEndingDate().isAfter(renewalWindowBoundary);
	}

	private Device resolveTicketDevice(License license, String deviceMac) {
		if (deviceMac != null && !deviceMac.isBlank()) {
			return findDeviceByMacAddress(normalizeMacAddress(deviceMac))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "device not found"));
		}

		return deviceLicenseRepository.findFirstByLicenseOrderByActivationDateAsc(license)
			.map(DeviceLicense::getDevice)
			.orElse(null);
	}

	private TicketResponse buildTicketResponse(License license, Device device, Instant now) {
		TicketDTO ticket = TicketDTO.builder()
			.serverDate(now)
			.ticketLifetimeSeconds(ticketLifetimeSeconds)
			.activationDate(license.getFirstActivationDate())
			.expirationDate(license.getEndingDate())
			.userId(license.getUser() == null ? null : license.getUser().getId())
			.deviceId(device == null ? null : device.getId())
			.blocked(license.isBlocked())
			.build();

		return TicketResponse.builder()
			.ticket(ticket)
			.signature(digitalSignatureService.sign(ticket))
			.build();
	}

	private LicenseResponseDTO toResponse(License license) {
		return LicenseResponseDTO.builder()
			.id(license.getId())
			.code(license.getCode())
			.productId(license.getProduct().getId())
			.typeId(license.getType().getId())
			.ownerId(license.getOwner().getId())
			.userId(license.getUser() == null ? null : license.getUser().getId())
			.firstActivationDate(license.getFirstActivationDate())
			.endingDate(license.getEndingDate())
			.blocked(license.isBlocked())
			.deviceCount(license.getDeviceCount())
			.description(license.getDescription())
			.build();
	}

	private String generateCode() {
		String code;
		do {
			code = UUID.randomUUID().toString().replace("-", "").toUpperCase();
		} while (licenseRepository.existsByCode(code));
		return code;
	}

	private java.util.Optional<Device> findDeviceByMacAddress(String normalizedMacAddress) {
		return deviceRepository.findByMacAddress(normalizedMacAddress)
			.or(() -> deviceRepository.findByMacAddress(normalizedMacAddress.replace(':', '-')));
	}

	private String normalizeMacAddress(String deviceMac) {
		return deviceMac.trim().replace('-', ':').toUpperCase();
	}
}
