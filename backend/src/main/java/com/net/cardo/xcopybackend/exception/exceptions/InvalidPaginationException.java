package com.net.cardo.xcopybackend.exception.exceptions;

public final class InvalidPaginationException extends RuntimeException {
    public InvalidPaginationException() {
        super("Неприпустимий розмір сторінки");
    }

    public InvalidPaginationException(String message) {
        super(message);
    }
}
