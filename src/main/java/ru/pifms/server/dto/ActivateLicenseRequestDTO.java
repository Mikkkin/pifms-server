package ru.pifms.server.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ActivateLicenseRequestDTO {

    @NotBlank(message = "activationKey is required")
    private String activationKey;

    @NotBlank(message = "deviceMac is required")
    private String deviceMac;

    @NotBlank(message = "deviceName is required")
    private String deviceName;
}