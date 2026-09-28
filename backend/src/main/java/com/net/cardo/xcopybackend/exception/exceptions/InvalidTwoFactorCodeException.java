package com.net.cardo.xcopybackend.exception.exceptions;

public class InvalidTwoFactorCodeException extends RuntimeException {
    public InvalidTwoFactorCodeException(String message) {
        super(message);
    }
}
