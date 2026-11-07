package com.net.cardo.xcopybackend.exception.exceptions;

public final class InvalidCommentException extends RuntimeException {
    public InvalidCommentException() {
        super("Коментар має містити від 1 до 5000 символів");
    }

    public InvalidCommentException(String message) {
        super(message);
    }
}
