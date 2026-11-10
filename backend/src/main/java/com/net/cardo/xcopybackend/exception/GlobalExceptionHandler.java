package com.net.cardo.xcopybackend.exception;

import com.net.cardo.xcopybackend.auth.dto.response.ErrorResponse;
import com.net.cardo.xcopybackend.exception.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdate(Exception ex) {
        return error(409, "Account changed during this request. Refresh and try again.");
    }

    private ResponseEntity<ErrorResponse> error(int status, String message) {
        return ResponseEntity.status(status).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ErrorResponse(message, status, System.currentTimeMillis()));
    }

    // Each domain type has its own mapping; services do not choose HTTP statuses.
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePostNotFound(PostNotFoundException ex) {
        return error(404, ex.getMessage());
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException ex) {
        return error(404, ex.getMessage());
    }

    @ExceptionHandler(InvalidPostException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPost(InvalidPostException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(InvalidCommentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidComment(InvalidCommentException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(PostAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handlePostAccessDenied(PostAccessDeniedException ex) {
        return error(403, ex.getMessage());
    }

    @ExceptionHandler(CommentAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCommentAccessDenied(CommentAccessDeniedException ex) {
        return error(403, ex.getMessage());
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationRequired(AuthenticationRequiredException ex) {
        return error(401, ex.getMessage());
    }

    @ExceptionHandler(InvalidPaginationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPagination(InvalidPaginationException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(InvalidAuthorException.class)
    public ResponseEntity<ErrorResponse> handleInvalidAuthor(InvalidAuthorException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCursor(InvalidCursorException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUsernameAlreadyExists(UsernameAlreadyExistsException ex) {
        return error(409, ex.getMessage());
    }

    @ExceptionHandler(InvalidLoginRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLoginRequest(InvalidLoginRequestException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(InvalidFollowRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFollowRequest(InvalidFollowRequestException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return error(409, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return error(404, ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPassword(InvalidPasswordException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(InvalidEmailException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEmail(InvalidEmailException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(InvalidUsernameException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUsername(InvalidUsernameException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotFound(EmailNotFoundException ex) {
        return error(404, ex.getMessage());
    }

    @ExceptionHandler(InvalidTwoFactorCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTwoFactorCode(InvalidTwoFactorCodeException ex) {
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return error(400, "Некоректне тіло запиту. Перевірте формат даних.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleArgumentType(MethodArgumentTypeMismatchException ex) {
        return error(400, "Некоректний формат параметра запиту.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return error(400, "Відсутній обов’язковий параметр запиту.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex) {
        return error(400, "Відсутня обов’язкова частина запиту.");
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ErrorResponse> handleRequestBinding(ServletRequestBindingException ex) {
        return error(ex.getStatusCode().value(), ex.getStatusCode().is5xxServerError()
                ? "Не вдалося обробити запит" : "Перевірте обов’язкові параметри та заголовки запиту.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
        return error(400, "Перевірте дані запиту.");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException ex) {
        // Return-value validation indicates a server bug, not invalid client input.
        return error(ex.getStatusCode().value(), ex.isForReturnValue()
                ? "Не вдалося обробити запит" : "Перевірте параметри запиту.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException ex) {
        return error(413, "Завеликий файл. Виберіть менший файл.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingResource(NoResourceFoundException ex) {
        return error(404, "Ресурс не знайдено");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingRoute(NoHandlerFoundException ex) {
        return error(404, "Ресурс не знайдено");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405).headers(ex.getHeaders()).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ErrorResponse("Цей HTTP-метод не підтримується", 405, System.currentTimeMillis()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedContentType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(415).headers(ex.getHeaders()).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ErrorResponse("Непідтримуваний формат запиту", 415, System.currentTimeMillis()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSecurityAccessDenied(AccessDeniedException ex) {
        return error(403, "Недостатньо прав для цієї дії");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleSecurityAuthentication(AuthenticationException ex) {
        return error(401, "Увійдіть в акаунт знову");
    }

    @ExceptionHandler({java.io.FileNotFoundException.class, java.nio.file.NoSuchFileException.class})
    public ResponseEntity<ErrorResponse> handleDisappearedFile(java.io.IOException ex, HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        // A missing media file is 404, while missing internal application files indicate a server fault.
        boolean media = path.startsWith("/media/posts/");
        return error(media ? 404 : 500, media ? "Медіафайл не знайдено" : "Не вдалося обробити запит");
    }

    @ExceptionHandler({org.springframework.dao.TransientDataAccessException.class,
            org.springframework.dao.DataAccessResourceFailureException.class,
            org.springframework.transaction.CannotCreateTransactionException.class,
            org.springframework.core.task.TaskRejectedException.class, org.springframework.mail.MailException.class})
    public ResponseEntity<ErrorResponse> handleTemporaryCapacityFailure(Exception ex) {
        return ResponseEntity.status(503).header(HttpHeaders.RETRY_AFTER, "2")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ErrorResponse("Сервіс тимчасово зайнятий. Спробуйте пізніше.", 503, System.currentTimeMillis()));
    }

    // Compatibility for existing auth/media services using explicit HTTP errors.
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        String message = status == 500 ? "Не вдалося обробити запит" : ex.getReason();
        if (message == null || message.isBlank()) message = statusMessage(ex.getStatusCode());
        return ResponseEntity.status(status).headers(ex.getHeaders()).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ErrorResponse(message, status, System.currentTimeMillis()));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleFrameworkError(ErrorResponseException ex) {
        int status = ex.getStatusCode().value();
        return ResponseEntity.status(status).headers(ex.getHeaders()).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ErrorResponse(statusMessage(ex.getStatusCode()), status, System.currentTimeMillis()));
    }

    private String statusMessage(HttpStatusCode code) {
        HttpStatus status = HttpStatus.resolve(code.value());
        return code.is5xxServerError() ? "Не вдалося обробити запит" : status == null ? "Помилка запиту" : status.getReasonPhrase();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected request failure", ex);
        // Unknown programming/storage failures must not become 400 or expose internal messages.
        return error(500, "Не вдалося обробити запит");
    }
}
