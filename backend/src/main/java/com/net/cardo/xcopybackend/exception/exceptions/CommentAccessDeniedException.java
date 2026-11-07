package com.net.cardo.xcopybackend.exception.exceptions;

public final class CommentAccessDeniedException extends RuntimeException {
    public CommentAccessDeniedException() {
        super("Можна видаляти лише власні коментарі");
    }

    public CommentAccessDeniedException(String message) {
        super(message);
    }
}
