package com.net.cardo.xcopybackend.exception.exceptions;

public final class InvalidAuthorException extends RuntimeException {
    public InvalidAuthorException() {
        super("Неправильний ID автора");
    }

    public InvalidAuthorException(String message) {
        super(message);
    }
}
