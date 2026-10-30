package com.net.cardo.xcopybackend.passwordreset.service;

import com.net.cardo.xcopybackend.auth.dto.request.LoginRequest;
import com.net.cardo.xcopybackend.auth.service.UserService;
import com.net.cardo.xcopybackend.config.FrontendLinks;
import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.exception.exceptions.InvalidPasswordException;
import com.net.cardo.xcopybackend.passwordreset.dto.request.ForgotPasswordRequest;
import com.net.cardo.xcopybackend.passwordreset.dto.request.ResetPasswordRequest;
import com.net.cardo.xcopybackend.passwordreset.entity.PasswordResetToken;
import com.net.cardo.xcopybackend.passwordreset.repository.PasswordResetTokenRepository;
import com.net.cardo.xcopybackend.twofactor.service.TwoFactorService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PasswordResetServiceTest {
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final EmailService mail = mock(EmailService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final PasswordResetService service = new PasswordResetService(tokens, users, encoder, mail,
            new FrontendLinks("https://app.example.test"));
    private User user;
    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.test");
        user.setPassword(encoder.encode("old-password"));
        token = new PasswordResetToken();
        token.setToken("valid-token");
        token.setUserId(user.getId());
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(tokens.findByToken(token.getToken())).thenReturn(Optional.of(token));
    }

    private ResetPasswordRequest request(String value, String password) {
        var request = new ResetPasswordRequest();
        request.setToken(value);
        request.setNewPassword(password);
        return request;
    }

    private void assertRejected(ResetPasswordRequest request) {
        var failure = assertThrows(ResponseStatusException.class, () -> service.resetPassword(request));
        assertEquals(400, failure.getStatusCode().value());
        assertNotNull(failure.getReason());
        verify(users, never()).save(any());
        verify(tokens, never()).save(any());
        assertTrue(encoder.matches("old-password", user.getPassword()));
    }

    @Test
    void emailLinkResetChangesPasswordAndAllowsEmailLogin() {
        when(tokens.save(any())).thenAnswer(invocation -> {
            PasswordResetToken saved = invocation.getArgument(0);
            when(tokens.findByToken(saved.getToken())).thenReturn(Optional.of(saved));
            return saved;
        });
        service.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
        var body = ArgumentCaptor.forClass(String.class);
        verify(mail).sendEmail(eq(user.getEmail()), eq("Reset password"), body.capture());
        String resetUrl = body.getValue().substring(body.getValue().indexOf("https://"));
        String resetToken = java.net.URI.create(resetUrl).getQuery().replaceFirst("^token=", "");
        assertFalse(resetToken.isBlank());
        assertEquals("/reset-password", java.net.URI.create(resetUrl).getPath());

        service.resetPassword(request(resetToken, "new-password"));
        var twoFactor = mock(TwoFactorService.class);
        var login = new UserService(users, encoder, twoFactor);
        assertEquals(user.getId(), login.loginOrRegister(new LoginRequest(user.getEmail(), "new-password")).getUserId());
        verify(twoFactor).beginLogin(user);
        assertThrows(InvalidPasswordException.class, () -> login.loginOrRegister(new LoginRequest(user.getEmail(), "old-password")));
        assertThrows(ResponseStatusException.class, () -> service.resetPassword(request(resetToken, "another-password")));
        assertTrue(encoder.matches("new-password", user.getPassword()));
    }

    @Test
    void rejectsMissingOrBlankTokens() {
        assertRejected(request(null, "new-password"));
        assertRejected(request(" ", "new-password"));
        verify(tokens, never()).findByToken(any());
    }

    @Test
    void rejectsUnknownToken() {
        assertRejected(request("unknown", "new-password"));
    }

    @Test
    void rejectsUsedToken() {
        token.setUsed(true);
        assertRejected(request(token.getToken(), "new-password"));
    }

    @Test
    void rejectsExpiredToken() {
        token.setExpiryDate(LocalDateTime.now().minusSeconds(1));
        assertRejected(request(token.getToken(), "new-password"));
    }

    @Test
    void rejectsTokenWithNoExpiry() {
        token.setExpiryDate(null);
        assertRejected(request(token.getToken(), "new-password"));
    }

    @Test
    void rejectsTokenForDeletedUser() {
        when(users.findById(user.getId())).thenReturn(Optional.empty());
        assertRejected(request(token.getToken(), "new-password"));
    }

    @Test
    void rejectsInvalidPasswordsWithoutConsumingToken() {
        for (String password : new String[] {null, "", "short", "        ", "a".repeat(73), "я".repeat(37)}) {
            assertRejected(request(token.getToken(), password));
        }
        assertFalse(token.isUsed());
    }
}
