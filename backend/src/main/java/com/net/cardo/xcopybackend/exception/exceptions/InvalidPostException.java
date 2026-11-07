package com.net.cardo.xcopybackend.exception.exceptions;

public final class InvalidPostException extends RuntimeException {
    public InvalidPostException() {
        super("Некоректний допис");
    }

    public InvalidPostException(String message) {
        super(message);
    }
}
