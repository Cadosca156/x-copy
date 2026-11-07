package com.net.cardo.xcopybackend.exception.exceptions;

public final class CommentNotFoundException extends RuntimeException {
    public CommentNotFoundException() {
        super("Коментар не знайдено");
    }

    public CommentNotFoundException(String message) {
        super(message);
    }
}
