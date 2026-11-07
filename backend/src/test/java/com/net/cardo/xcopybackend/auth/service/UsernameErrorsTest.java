package com.net.cardo.xcopybackend.auth.service;

import com.net.cardo.xcopybackend.exception.exceptions.InvalidUsernameException;
import com.net.cardo.xcopybackend.exception.exceptions.UsernameAlreadyExistsException;
import com.net.cardo.xcopybackend.security.EphemeralAuthStore;
import com.net.cardo.xcopybackend.twofactor.service.TwoFactorService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsernameErrorsTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserService service = new UserService(users, mock(PasswordEncoder.class),
            mock(TwoFactorService.class), mock(EphemeralAuthStore.class));

    @Test void missingOrInvalidUsernameIsAValidationErrorBeforeAnyDatabaseWork() {
        for (String username : new String[]{null, "", "  ", "ab", "invalid name", "a".repeat(31)}) {
            assertThrows(InvalidUsernameException.class, () -> service.updateUsername(new User(), username));
        }
        verifyNoInteractions(users);
    }

    @Test void takenUsernameIsAConflictAndDoesNotSave() {
        when(users.existsByUsernameIgnoreCase("taken")).thenReturn(true);
        assertThrows(UsernameAlreadyExistsException.class, () -> service.updateUsername(new User(), " TAKEN "));
        verify(users, never()).save(any());
    }
}
