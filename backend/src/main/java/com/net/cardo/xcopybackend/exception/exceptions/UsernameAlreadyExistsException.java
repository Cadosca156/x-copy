package com.net.cardo.xcopybackend.exception.exceptions;

public final class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() {
        super("Це ім’я користувача вже зайняте");
    }

    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
