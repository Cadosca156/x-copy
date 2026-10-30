package com.net.cardo.xcopybackend.auth.service;

import com.net.cardo.xcopybackend.auth.dto.request.LoginRequest;
import com.net.cardo.xcopybackend.exception.GlobalExceptionHandler;
import com.net.cardo.xcopybackend.exception.exceptions.InvalidPasswordException;
import com.net.cardo.xcopybackend.twofactor.service.TwoFactorService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final TwoFactorService twoFactor = mock(TwoFactorService.class);
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(4);
    private final UserService service = new UserService(users, passwords, twoFactor);
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.test");
        user.setPassword(passwords.encode("correct-password"));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void acceptsCorrectPasswordAndSendsVerificationCode() {
        var result = service.loginOrRegister(new LoginRequest(user.getEmail(), "correct-password"));
        assertEquals(user.getId(), result.getUserId());
        verify(twoFactor).beginLogin(user);
        verify(users, never()).save(any());
    }

    @Test
    void rejectsWrongPasswordWithAnExplanationAndDoesNotSendCode() {
        var failure = assertThrows(InvalidPasswordException.class,
                () -> service.loginOrRegister(new LoginRequest(user.getEmail(), "wrong-password")));
        assertNotNull(failure.getMessage());
        assertFalse(failure.getMessage().isBlank());
        verifyNoInteractions(twoFactor);
        verify(users, never()).save(any());
    }

    @Test
    void rejectsPasswordLoginForGoogleAccountWithoutSettingAPassword() {
        user.setPassword(null);
        assertThrows(InvalidPasswordException.class,
                () -> service.loginOrRegister(new LoginRequest(user.getEmail(), "any-password")));
        assertNull(user.getPassword());
        verifyNoInteractions(twoFactor);
        verify(users, never()).save(any());
    }

    @Test
    void rejectsAccountWithBlankPasswordHash() {
        user.setPassword(" ");
        assertThrows(InvalidPasswordException.class,
                () -> service.loginOrRegister(new LoginRequest(user.getEmail(), "any-password")));
        verifyNoInteractions(twoFactor);
    }

    @Test
    void includesExplanationInBadRequestResponse() {
        var response = new GlobalExceptionHandler().handlePasswordValid(new InvalidPasswordException());
        assertEquals(400, response.getStatusCode().value());
        var body = assertInstanceOf(com.net.cardo.xcopybackend.auth.dto.response.ErrorResponse.class,
                response.getBody());
        assertEquals(400, body.getStatus());
        assertNotNull(body.getMessage());
        assertFalse(body.getMessage().isBlank());
    }
}
