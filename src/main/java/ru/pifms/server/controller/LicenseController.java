package ru.pifms.server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ru.pifms.server.config.AuthenticatedUserPrincipal;
import ru.pifms.server.dto.ActivateLicenseRequestDTO;
import ru.pifms.server.dto.CheckLicenseRequestDTO;
import ru.pifms.server.dto.CreateLicenseRequestDTO;
import ru.pifms.server.dto.LicenseResponseDTO;
import ru.pifms.server.dto.RenewLicenseRequestDTO;
import ru.pifms.server.dto.TicketResponse;
import ru.pifms.server.service.ApplicationUserService;
import ru.pifms.server.service.LicenseService;

@RestController
@RequestMapping("/api/licenses")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;
    private final ApplicationUserService applicationUserService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LicenseResponseDTO> createLicense(@Valid @RequestBody CreateLicenseRequestDTO request) {
        LicenseResponseDTO response = licenseService.createLicense(request, requireUserId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/activate")
    public ResponseEntity<TicketResponse> activateLicense(@Valid @RequestBody ActivateLicenseRequestDTO request) {
        return ResponseEntity.ok(licenseService.activateLicense(request, requireUserId()));
    }

    @PostMapping("/check")
    public ResponseEntity<TicketResponse> checkLicense(@Valid @RequestBody CheckLicenseRequestDTO request) {
        return ResponseEntity.ok(licenseService.checkLicense(request, requireUserId()));
    }

    @PostMapping("/renew")
    public ResponseEntity<TicketResponse> renewLicense(@Valid @RequestBody RenewLicenseRequestDTO request) {
        return ResponseEntity.ok(licenseService.renewLicense(request, requireUserId()));
    }

    private Long requireUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof AuthenticatedUserPrincipal authenticatedUserPrincipal
			&& authenticatedUserPrincipal.id() != null) {
			return authenticatedUserPrincipal.id();
		}

		if (principal instanceof UserDetails userDetails) {
			return applicationUserService.getUserByUsernameOrFail(userDetails.getUsername()).getId();
		}

		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}