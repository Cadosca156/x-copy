package com.net.cardo.xcopybackend.twofactor.service;

import com.net.cardo.xcopybackend.security.*;
import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TwoFactorResetSecurityTest {
    private final EphemeralAuthStore store = mock(EphemeralAuthStore.class);
    private final EmailService mail = mock(EmailService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final TwoFactorCodeGenerator generator = mock(TwoFactorCodeGenerator.class);
    private final TwoFactorService service = new TwoFactorService(mail,generator,users,jwt,store,encoder);
    private final String challenge = "c".repeat(43);
    private final String key = "security:challenge:"+EphemeralAuthStore.digest(challenge);
    private User prepare(int version) {
        var user = new User(); user.setId(UUID.randomUUID());user.setEmail("user@example.test");user.setTokenVersion(version);
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        when(store.get(key)).thenReturn(user.getId()+"|0|"+encoder.encode("123456"));
        return user;
    }
    @Test void expiredChallengeCannotSendMailOrIssueASession() {
        assertThrows(ResponseStatusException.class,()->service.resend(challenge));
        assertThrows(ResponseStatusException.class,()->service.verifyCode(challenge,"123456"));
        verifyNoInteractions(mail,users,jwt);
    }
    @Test void resetOrLogoutInvalidatesBothVerificationAndResend() {
        prepare(1);
        assertThrows(ResponseStatusException.class,()->service.verifyCode(challenge,"123456"));
        assertThrows(ResponseStatusException.class,()->service.resend(challenge));
        verifyNoInteractions(jwt,mail);
        verify(store,never()).consume(anyString(),anyString());
    }
    @Test void resendingACodeCannotResetAnExhaustedAttemptBudget() {
        prepare(0);
        when(generator.generateTwoFactorCode()).thenReturn("654321");
        when(store.replace(eq(key),anyString(),anyString())).thenReturn(true);
        doThrow(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).when(store)
            .limit("otp:"+key,5,Duration.ofMinutes(5));
        service.resend(challenge);
        assertEquals(429,assertThrows(ResponseStatusException.class,()->
            service.verifyCode(challenge,"654321")).getStatusCode().value());
        verify(mail).sendEmail(eq("user@example.test"),anyString(),contains("654321"));
        // The service replaces only a live value; it never creates a fresh challenge/TTL.
        verify(store,never()).put(anyString(),anyString(),any());
        verify(store,never()).delete(anyString()); verifyNoInteractions(jwt);
    }
    @Test void resendRacingWithConsumptionCannotResurrectAChallenge() {
        prepare(0);when(generator.generateTwoFactorCode()).thenReturn("654321");
        when(store.replace(eq(key),anyString(),anyString())).thenReturn(false);
        assertThrows(ResponseStatusException.class,()->service.resend(challenge));
        verifyNoInteractions(mail,jwt);
    }
}
