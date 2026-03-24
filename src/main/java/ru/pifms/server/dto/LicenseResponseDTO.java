package ru.pifms.server.dto;

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
public class LicenseResponseDTO {

	private UUID id;
	private String code;
	private UUID productId;
	private UUID typeId;
	private Long ownerId;
	private Long userId;
	private Instant firstActivationDate;
	private Instant endingDate;
	private boolean blocked;
	private int deviceCount;
	private String description;
}
