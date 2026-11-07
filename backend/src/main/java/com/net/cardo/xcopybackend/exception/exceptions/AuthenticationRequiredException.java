package com.net.cardo.xcopybackend.exception.exceptions;

public final class AuthenticationRequiredException extends RuntimeException {
    public AuthenticationRequiredException() {
        super("Увійдіть в акаунт знову");
    }

    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
