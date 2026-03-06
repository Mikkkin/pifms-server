package ru.pifms.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class RegisterDTO {

    @NotBlank(message = "Username should not be empty")
    @Size(min = 3, max = 50, message = "Length of username should be upper than 3")
    private String username;

    @NotBlank(message = "Email should not be empty")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password should not be empty")
    @Size(min = 8, max = 100, message = "Lenght of password should be upper than 8")
    private String password;

    @NotBlank(message = "Password confirmation should not be empty")
    private String passwordConfirm;
}
