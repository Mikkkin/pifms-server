package ru.pifms.server.exception;

public class UserAlreadyExist extends RuntimeException {
    public UserAlreadyExist(String message) {
        super(message);
    }

    public UserAlreadyExist(String message, Throwable cause) {
        super(message, cause);
    }
}
