package ru.pifms.server.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ru.pifms.server.exception.InvalidPassword;


@Service
public class PasswordService {
    
    private static final Pattern uppercase = Pattern.compile("[A-Z]");
    private static final Pattern lowercase = Pattern.compile("[a-z]");
    private static final Pattern digits = Pattern.compile("[0-9]");
    private static final Pattern spechr = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?]");

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 100;


    public void validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new InvalidPassword("Password cannot be empty");
        }

        if (password.length() < MIN_LENGTH) {
            throw new InvalidPassword(
                String.format("The password must contain more than %d characters", MIN_LENGTH)
            );
        }

        if (password.length() > MAX_LENGTH) {
            throw new InvalidPassword(
                String.format("The password must be less than %d characters", MAX_LENGTH)
            );
        }

        if (!uppercase.matcher(password).find()) {
            throw new InvalidPassword(
                "Password must contain at least one uppercase letter"
            );
        }

        if (!lowercase.matcher(password).find()) {
            throw new InvalidPassword(
                "Password must contain at least one lowercase letter"
            );
        }

        if (!digits.matcher(password).find()) {
            throw new InvalidPassword(
                "Password must contain at least one digit"
            );
        }

        if (!spechr.matcher(password).find()) {
            throw new InvalidPassword(
                "Password must contain at least one special character"
            );
        }
    }


    public void validatePasswordMatch(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new InvalidPassword("Passwords do not match");
        }
    }
}
