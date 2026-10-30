package com.net.cardo.xcopybackend.passwordreset.service;

import com.net.cardo.xcopybackend.config.FrontendLinks;
import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.passwordreset.controller.PasswordResetController;
import com.net.cardo.xcopybackend.passwordreset.dto.request.*;
import com.net.cardo.xcopybackend.passwordreset.entity.PasswordResetToken;
import com.net.cardo.xcopybackend.passwordreset.repository.PasswordResetTokenRepository;
import com.net.cardo.xcopybackend.security.EphemeralAuthStore;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PasswordResetSecurityUnitTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
    private final EmailService mail = mock(EmailService.class);
    private final EphemeralAuthStore store = mock(EphemeralAuthStore.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final PasswordResetService service = new PasswordResetService(tokens,users,encoder,mail,
        new FrontendLinks("https://app.example.test"),store);
    private User user() {
        var user = new User(); user.setId(UUID.randomUUID());user.setEmail("known@example.test");return user;
    }
    @Test void knownAndUnknownEmailReturnIdenticalPublicResponses() {
        var user = user();when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findByEmail("unknown@example.test")).thenReturn(Optional.empty());
        var controller = new PasswordResetController(service);
        var known = controller.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
        var unknown = controller.forgotPassword(new ForgotPasswordRequest("unknown@example.test"));
        assertEquals(202,known.getStatusCode().value());assertEquals(known,unknown);
        assertNull(unknown.getBody());
        verify(mail).sendEmail(eq(user.getEmail()),anyString(),anyString());
        verify(mail,never()).sendEmail(eq("unknown@example.test"),anyString(),anyString());
    }
    @Test void successfulResetRevokesAllSessionsAndAllOtherResetLinks() {
        var user = user();var token = new PasswordResetToken();
        token.setUserId(user.getId());token.setToken("reset-link");
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        when(tokens.findByToken("reset-link")).thenReturn(Optional.of(token));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        var request = new ResetPasswordRequest();request.setToken("reset-link");request.setNewPassword("new-password");
        service.resetPassword(request);
        assertTrue(encoder.matches("new-password",user.getPassword()));assertTrue(token.isUsed());
        verify(users).invalidateSessions(user.getEmail());verify(tokens).invalidateForUser(user.getId());
        assertThrows(ResponseStatusException.class,()->service.resetPassword(request));
        verify(users,times(1)).invalidateSessions(user.getEmail());
    }
    @Test void recoveryMailLimitAppliesBeforeLookupAndDelivery() {
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).when(store)
            .limit("forgot:known@example.test",3,Duration.ofMinutes(15));
        assertThrows(ResponseStatusException.class,()->service.forgotPassword(new ForgotPasswordRequest("KNOWN@example.test")));
        verifyNoInteractions(users,tokens,mail);
    }
}
