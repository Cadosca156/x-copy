package com.net.cardo.xcopybackend.exception.exceptions;

public final class InvalidFollowRequestException extends RuntimeException {
    public InvalidFollowRequestException() {
        super("Не можна підписатися на себе");
    }

    public InvalidFollowRequestException(String message) {
        super(message);
    }
}
