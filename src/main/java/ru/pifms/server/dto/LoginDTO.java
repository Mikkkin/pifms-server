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
public class LoginDTO {

    @NotBlank(message = "Name cannot be empty")
    private String username;

    @NotBlank(message = "Password cannot be empty")
    private String password;
}
