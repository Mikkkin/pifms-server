package ru.pifms.server.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
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
public class CheckLicenseRequestDTO {

    @NotBlank(message = "deviceMac is required")
    private String deviceMac;

    @NotNull(message = "productId is required")
    private UUID productId;
}