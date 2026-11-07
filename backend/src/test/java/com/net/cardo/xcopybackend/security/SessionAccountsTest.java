package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionAccountsTest {
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    @Test void deletedAccountCannotReattachToAnotherAccountWithTheSameEmail() {
        var users = mock(UserRepository.class);var oldId = UUID.randomUUID();String email = "user@example.test";
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new SessionPrincipal(oldId, email), null, List.of()));
        when(users.findById(oldId)).thenReturn(Optional.empty());
        assertTrue(SessionAccounts.find(users, email).isEmpty());
        verify(users, never()).findByEmail(anyString());
    }
    @Test void resourceLookupUsesTheSessionAccountIdAndRejectsDifferentEmails() {
        var users = mock(UserRepository.class);var user = new User();user.setId(UUID.randomUUID());user.setEmail("user@example.test");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new SessionPrincipal(user.getId(), user.getEmail()), null, List.of()));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        assertEquals(Optional.of(user), SessionAccounts.find(users, user.getEmail()));
        assertTrue(SessionAccounts.find(users, "other@example.test").isEmpty());
        verify(users, never()).findByEmail(anyString());
    }
}
