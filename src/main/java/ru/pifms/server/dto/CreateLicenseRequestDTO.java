package ru.pifms.server.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class CreateLicenseRequestDTO {

	@NotNull(message = "productId is required")
	private UUID productId;

	@NotNull(message = "typeId is required")
	private UUID typeId;

	@NotNull(message = "ownerId is required")
	private Long ownerId;

	@Min(value = 1, message = "deviceCount must be greater than 0")
	private int deviceCount;

	private String description;
}
