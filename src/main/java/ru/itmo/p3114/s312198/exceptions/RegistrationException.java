package ru.itmo.p3114.s312198.exceptions;

public class RegistrationException extends AuthenticationException {
    public RegistrationException() {
        super("Unable to register");
    }

    public RegistrationException(String message) {
        super(message);
    }
}
