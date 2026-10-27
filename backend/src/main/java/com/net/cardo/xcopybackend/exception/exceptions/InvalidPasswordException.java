package com.net.cardo.xcopybackend.exception.exceptions;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }

    public InvalidPasswordException() {
        super("Неправильний email або пароль. Спробуйте вхід через Google або відновіть пароль.");
    }
}
