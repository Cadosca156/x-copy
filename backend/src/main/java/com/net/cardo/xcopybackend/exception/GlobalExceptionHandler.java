package com.net.cardo.xcopybackend.exception;

import com.net.cardo.xcopybackend.auth.dto.response.ErrorResponse;
import com.net.cardo.xcopybackend.exception.exceptions.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({java.io.FileNotFoundException.class,java.nio.file.NoSuchFileException.class})
    public ResponseEntity<ErrorResponse> handleDisappearedFile(java.io.IOException failure,jakarta.servlet.http.HttpServletRequest request) {
        String path=request.getServletPath();
        if(path==null || path.isEmpty())path=request.getRequestURI().substring(request.getContextPath().length());
        // Files can disappear between resource resolution and reading metadata/content.
        boolean media=path.startsWith("/media/posts/");
        return ResponseEntity.status(media?HttpStatus.NOT_FOUND:HttpStatus.INTERNAL_SERVER_ERROR)
                .header("Cache-Control","no-store")
                .body(new ErrorResponse(media?"Медіафайл не знайдено":"Не вдалося обробити запит",media?404:500,System.currentTimeMillis()));
    }

    @ExceptionHandler({org.springframework.dao.TransientDataAccessException.class,
            org.springframework.dao.DataAccessResourceFailureException.class,
            org.springframework.transaction.CannotCreateTransactionException.class,
            org.springframework.core.task.TaskRejectedException.class,org.springframework.mail.MailException.class})
    public ResponseEntity<ErrorResponse> handleTemporaryCapacityFailure(RuntimeException failure) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).header("Retry-After","2")
                .header("Cache-Control","no-store")
                .body(new ErrorResponse("Сервіс тимчасово зайнятий. Спробуйте пізніше.",503,System.currentTimeMillis()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ErrorResponse(
                        ex.getReason(),
                        ex.getStatusCode().value(),
                        System.currentTimeMillis()
                ));
    }


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
