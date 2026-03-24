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
public class TicketDTO {

	private Instant serverDate;
	private long ticketLifetimeSeconds;
	private Instant activationDate;
	private Instant expirationDate;
	private Long userId;
	private UUID deviceId;
	private boolean blocked;
}
