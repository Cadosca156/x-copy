package com.net.cardo.xcopybackend.exception.exceptions;

public final class PostAccessDeniedException extends RuntimeException {
    public PostAccessDeniedException() {
        super("Можна видаляти лише власні дописи");
    }

    public PostAccessDeniedException(String message) {
        super(message);
    }
}
