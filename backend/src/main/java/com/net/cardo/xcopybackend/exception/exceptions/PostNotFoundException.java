package com.net.cardo.xcopybackend.exception.exceptions;

public final class PostNotFoundException extends RuntimeException {
    public PostNotFoundException() {
        super("Допис не знайдено");
    }

    public PostNotFoundException(String message) {
        super(message);
    }
}
