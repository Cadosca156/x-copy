package com.net.cardo.xcopybackend.exception;

import com.net.cardo.xcopybackend.dto.response.ErrorResponse;
import com.net.cardo.xcopybackend.exception.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<?> handleEmailExists(EmailAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        ex.getMessage(),
                        HttpStatus.CONFLICT.value(),
                        System.currentTimeMillis()
                ));

    }
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<?> handleUserFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        ex.getMessage(),
                        HttpStatus.NOT_FOUND.value(),
                        System.currentTimeMillis()
                ));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<?> handlePasswordValid(InvalidPasswordException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ex.getMessage(),
                        HttpStatus.BAD_REQUEST.value(),
                        System.currentTimeMillis()
                ));
    }
    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<?> handleInvalidEmail(InvalidEmailException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ex.getMessage(),
                        HttpStatus.BAD_REQUEST.value(),
                        System.currentTimeMillis()
                ));

    }
    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<?> handleUsernameValid(InvalidUsernameException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ex.getMessage(),
                        HttpStatus.BAD_REQUEST.value(),
                        System.currentTimeMillis()
                ));

}
    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<?> handelEmailNotFound(EmailNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        ex.getMessage(),
                        HttpStatus.NOT_FOUND.value(),
                        System.currentTimeMillis()
                ));

    }
    @ExceptionHandler(InvalidTwoFactorCodeException.class)
    public ResponseEntity<?> handelInvalidTwoFactorCode(InvalidTwoFactorCodeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(
                        ex.getMessage(),
                        HttpStatus.BAD_GATEWAY.value(),
                        System.currentTimeMillis()
                ));

    }




}
