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
public class RenewLicenseRequestDTO {

    @NotBlank(message = "activationKey is required")
    private String activationKey;

    private String deviceMac;
}