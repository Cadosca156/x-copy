package com.net.cardo.xcopybackend.exception.exceptions;

public final class InvalidLoginRequestException extends RuntimeException {
    public InvalidLoginRequestException() {
        super("Перевірте email та пароль");
    }

    public InvalidLoginRequestException(String message) {
        super(message);
    }
}
