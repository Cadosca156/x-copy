package com.net.cardo.xcopybackend.exception;

import com.net.cardo.xcopybackend.exception.exceptions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TypedExceptionHandlerTest {
    @RestController
    static class Endpoint {
        private final Exception failure;
        Endpoint(Exception failure) { this.failure = failure; }
        @GetMapping("/failure") Object fail() throws Exception { throw failure; }
        @PostMapping(value = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        Object body(@RequestBody java.util.Map<String, String> body) { return body; }
        @GetMapping("/parameter") int parameter(@RequestParam int size) { return size; }
        @GetMapping("/header") String header(@RequestHeader("X-Required") String value) { return value; }
    }

    private MockMvc mvc(Exception failure) {
        return MockMvcBuilders.standaloneSetup(new Endpoint(failure))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    static Stream<Arguments> domainErrors() {
        return Stream.of(
                Arguments.of(new PostNotFoundException(), 404),
                Arguments.of(new CommentNotFoundException(), 404),
                Arguments.of(new InvalidPostException("Додай текст, фото або відео"), 400),
                Arguments.of(new InvalidCommentException(), 400),
                Arguments.of(new PostAccessDeniedException(), 403),
                Arguments.of(new CommentAccessDeniedException(), 403),
                Arguments.of(new AuthenticationRequiredException(), 401),
                Arguments.of(new InvalidPaginationException(), 400),
                Arguments.of(new InvalidAuthorException(), 400),
                Arguments.of(new InvalidCursorException(), 400),
                Arguments.of(new UsernameAlreadyExistsException(), 409),
                Arguments.of(new InvalidLoginRequestException(), 400),
                Arguments.of(new InvalidFollowRequestException(), 400),
                Arguments.of(new EmailAlreadyExistsException("Email зайнятий"), 409),
                Arguments.of(new UserNotFoundException("Користувача не знайдено"), 404),
                Arguments.of(new InvalidPasswordException(), 400),
                Arguments.of(new InvalidEmailException("Некоректний email"), 400),
                Arguments.of(new InvalidUsernameException("Некоректне ім’я"), 400),
                Arguments.of(new EmailNotFoundException("Email не знайдено"), 404),
                Arguments.of(new InvalidTwoFactorCodeException("Неправильний код"), 400));
    }

    @ParameterizedTest @MethodSource("domainErrors")
    void mapsEachDomainTypeToItsOwnStatusAndPublicMessage(Exception failure, int code) throws Exception {
        mvc(failure).perform(get("/failure")).andExpect(status().is(code))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.message").value(failure.getMessage()))
                .andExpect(jsonPath("$.status").value(code)).andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test void malformedJsonDoesNotExposeParserDetails() throws Exception {
        mvc(null).perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{password:secret"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Некоректне тіло запиту. Перевірте формат даних."));
    }

    @Test void missingAndWronglyTypedParametersAreBadRequests() throws Exception {
        mvc(null).perform(get("/parameter")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Відсутній обов’язковий параметр запиту."));
        mvc(null).perform(get("/parameter").param("size", "not-an-integer"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc(null).perform(get("/header")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test void missingRoutesRemain404() throws Exception {
        mvc(null).perform(get("/not-a-route")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test void methodAndContentTypeErrorsPreserveProtocolStatusesAndHeaders() throws Exception {
        mvc(null).perform(put("/body")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "POST")).andExpect(jsonPath("$.status").value(405));
        mvc(null).perform(post("/body").contentType(MediaType.TEXT_PLAIN).content("text"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.status").value(415));
    }

    @Test void unknownProgrammingErrorsRemainServerErrorsWithNoInternalMessage() throws Exception {
        mvc(new IllegalArgumentException("internal-secret-query"))
                .perform(get("/failure")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Не вдалося обробити запит"))
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test void knownHttpErrorsRemainCompatibleAndInternalReasonsAreHidden() throws Exception {
        mvc(new ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "Забагато спроб"))
                .perform(get("/failure")).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Забагато спроб"));
        mvc(new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "private-file-path"))
                .perform(get("/failure")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Не вдалося обробити запит"));
    }

    @Test void transientInfrastructureFailureIsRetryableAndHidesInternalDetails() throws Exception {
        mvc(new org.springframework.dao.DataAccessResourceFailureException("private-db-address"))
                .perform(get("/failure")).andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "2"))
                .andExpect(jsonPath("$.message").value("Сервіс тимчасово зайнятий. Спробуйте пізніше."));
    }

    @Test void uploadAndSecurityFailuresHaveSpecificHandlers() throws Exception {
        mvc(new org.springframework.web.multipart.MaxUploadSizeExceededException(1)).perform(get("/failure"))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.status").value(413));
        mvc(new org.springframework.security.access.AccessDeniedException("internal-role"))
                .perform(get("/failure")).andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mvc(new org.springframework.security.authentication.BadCredentialsException("private-hash"))
                .perform(get("/failure")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
    }
}
