package ru.itmo.p3114.s312198.exceptions;

public class AuthenticationException extends RuntimeException {
    public AuthenticationException() {
        super("An authentication exception occurred");
    }

    public AuthenticationException(String message) {
        super(message);
    }
}
