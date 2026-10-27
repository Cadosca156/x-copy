package com.net.cardo.xcopybackend.auth.service;

import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountPasswordServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AccountPasswordService service = new AccountPasswordService(users, encoder);
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("google@example.test");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void reportsWhetherAPasswordExistsWithoutReturningTheHash() {
        assertFalse(service.getStatus(user.getEmail()).passwordSet());
        user.setPassword(encoder.encode("existing-password"));
        assertTrue(service.getStatus(user.getEmail()).passwordSet());
    }

    @Test
    void storesAHashThatWorksForSubsequentEmailLogin() {
        String password = " new-password ";
        when(users.setInitialPassword(eq(user.getEmail()), anyString())).thenReturn(1);
        assertTrue(service.setInitialPassword(user.getEmail(), password).passwordSet());
        verify(users).setInitialPassword(eq(user.getEmail()), argThat(hash ->
                !password.equals(hash) && encoder.matches(password, hash)));
        verify(users, never()).save(any());
    }

    @Test
    void rejectsReplacingAnExistingPassword() {
        user.setPassword(encoder.encode("existing-password"));
        var failure = assertThrows(ResponseStatusException.class,
                () -> service.setInitialPassword(user.getEmail(), "replacement-password"));
        assertEquals(409, failure.getStatusCode().value());
        verify(users, never()).setInitialPassword(anyString(), anyString());
    }

    @Test
    void rejectsInvalidPasswordsBeforeUpdatingAnything() {
        for (String invalid : new String[] {null, "", "        ", "short", "a".repeat(73), "я".repeat(37)}) {
            var failure = assertThrows(ResponseStatusException.class,
                    () -> service.setInitialPassword(user.getEmail(), invalid));
            assertEquals(400, failure.getStatusCode().value());
        }
        verify(users, never()).setInitialPassword(anyString(), anyString());
    }

    @Test
    void rejectsASecondRequestIfAnotherRequestAlreadySetThePassword() {
        when(users.setInitialPassword(eq(user.getEmail()), anyString())).thenReturn(0);
        var failure = assertThrows(ResponseStatusException.class,
                () -> service.setInitialPassword(user.getEmail(), "new-password"));
        assertEquals(409, failure.getStatusCode().value());
    }

    @Test
    void rejectsAnUnknownAuthenticatedAccount() {
        var failure = assertThrows(ResponseStatusException.class,
                () -> service.setInitialPassword("missing@example.test", "new-password"));
        assertEquals(404, failure.getStatusCode().value());
        verify(users, never()).setInitialPassword(anyString(), anyString());
    }
}
