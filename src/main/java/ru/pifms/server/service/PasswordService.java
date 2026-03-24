package ru.pifms.server.service;

import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ru.pifms.server.exception.InvalidPassword;


@Service
public class PasswordService {
    
    private static final Pattern UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE = Pattern.compile("[a-z]");
    private static final Pattern DIGITS = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHARS = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>?]");

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

        if (!UPPERCASE.matcher(password).find()) {
            throw new InvalidPassword(
                "Password must contain at least one uppercase letter"
            );
        }

        if (!LOWERCASE.matcher(password).find()) {
            throw new InvalidPassword(
                "Password must contain at least one lowercase letter"
            );
        }

        if (!DIGITS.matcher(password).find()) {
            throw new InvalidPassword(
                "Password must contain at least one digit"
            );
        }

        if (!SPECIAL_CHARS.matcher(password).find()) {
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
