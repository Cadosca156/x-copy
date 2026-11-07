package com.net.cardo.xcopybackend.exception.exceptions;

public final class InvalidCursorException extends RuntimeException {
    public InvalidCursorException() {
        super("Неправильний курсор стрічки. Оновіть сторінку.");
    }

    public InvalidCursorException(String message) {
        super(message);
    }
}
