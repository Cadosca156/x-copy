package com.net.cardo.xcopybackend.auth;

import com.net.cardo.xcopybackend.auth.dto.request.LoginRequest;
import com.net.cardo.xcopybackend.auth.service.*;
import com.net.cardo.xcopybackend.config.FrontendLinks;
import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.passwordreset.dto.request.ForgotPasswordRequest;
import com.net.cardo.xcopybackend.passwordreset.repository.PasswordResetTokenRepository;
import com.net.cardo.xcopybackend.passwordreset.service.PasswordResetService;
import com.net.cardo.xcopybackend.security.*;
import com.net.cardo.xcopybackend.twofactor.service.*;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailCaseFlowTest {
    private final UserRepository users=mock(UserRepository.class,CALLS_REAL_METHODS);
    private final PasswordEncoder passwords=mock(PasswordEncoder.class);
    private final TwoFactorService twoFactor=mock(TwoFactorService.class);
    private final EphemeralAuthStore store=mock(EphemeralAuthStore.class);
    private final UserService login=new UserService(users,passwords,twoFactor,store);
    private User user() {
        var user=new User();user.setId(UUID.randomUUID());user.setEmail("Owner@Example.TEST");user.setPassword("hash");
        when(users.findByEmailKey("owner@example.test")).thenReturn(Optional.of(user));return user;
    }
    @AfterEach void clear() {SecurityContextHolder.clearContext();}

    @Test void caseVariantsLogIntoTheExistingAccountAndNeverStartRegistration() {
        var user=user();when(passwords.matches("correct","hash")).thenReturn(true);
        for(String email:new String[]{"owner@example.test","OWNER@EXAMPLE.TEST"," Owner@Example.Test "})
            assertEquals(user.getId(),login.loginOrRegister(new LoginRequest(email,"correct")).getUserId());
        verify(twoFactor,times(3)).beginLogin(user);verify(twoFactor,never()).beginRegistration(any());
        verify(users,never()).save(any());verify(users,never()).saveAndFlush(any());
        verify(store,times(3)).limit(eq("password:owner@example.test"),eq(10),any());
    }

    @Test void pendingRegistrationKeepsCleanedDeliverySpellingAndHasNoPersistentAccount() {
        when(passwords.encode("new-password")).thenReturn("hash");
        assertNull(login.loginOrRegister(new LoginRequest("  New.Name+Tag@Example.TEST  ","new-password")).getUserId());
        verify(twoFactor).beginRegistration(argThat(u->u.getEmail().equals("New.Name+Tag@Example.TEST")&&u.getId()==null));
        verify(users,never()).save(any());
    }

    @Test void recoveryFindsAnyCaseButSendsToTheStoredAddress() {
        var user=user();var mail=mock(EmailService.class);
        var recovery=new PasswordResetService(mock(PasswordResetTokenRepository.class),users,passwords,mail,
                new FrontendLinks("https://app.example.test"),store,mock(BrowserSessionService.class));
        recovery.forgotPassword(new ForgotPasswordRequest("  OWNER@example.test  "));
        verify(mail).sendEmail(eq(user.getEmail()),eq("Reset password"),anyString());
        verify(store).limit(eq("forgot:owner@example.test"),eq(3),any());
    }

    @Test void googleInitialPasswordUpdateUsesTheSameKeyAndSessionsStayBoundToUserId() {
        var user=user();user.setPassword(null);when(passwords.encode(anyString())).thenReturn("hash");
        when(users.setInitialPassword("owner@example.test","hash")).thenReturn(1);
        assertTrue(new AccountPasswordService(users,passwords).setInitialPassword("OWNER@example.TEST","new-password").passwordSet());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new SessionPrincipal(user.getId(),"OWNER@EXAMPLE.TEST"),null,List.of()));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        assertEquals(Optional.of(user),SessionAccounts.find(users,"owner@example.test"));
        when(users.findById(user.getId())).thenReturn(Optional.empty());
        assertTrue(SessionAccounts.find(users,"owner@example.test").isEmpty());
    }

    @Test void anExistingGoogleAccountUnderDifferentCaseCannotInheritPendingCredentials() {
        var user=user();user.setPassword(null);
        var generator=mock(TwoFactorCodeGenerator.class);var mail=mock(EmailService.class);
        var service=new TwoFactorService(mail,generator,users,store,passwords);
        String challenge=EphemeralAuthStore.random(),key="security:challenge:"+EphemeralAuthStore.digest(challenge);
        String email=Base64.getUrlEncoder().withoutPadding().encodeToString("OWNER@example.test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        when(store.get(key)).thenReturn(UUID.randomUUID()+"|0|otp-hash|"+email+"|pending-password");
        when(passwords.matches("123456","otp-hash")).thenReturn(true);
        assertThrows(ResponseStatusException.class,()->service.verifyCode(challenge,"123456"));
        verify(users,never()).saveAndFlush(any());assertNull(user.getPassword());
    }

    @Test void aConcurrentVerifiedRegistrationReturnsAnEmailConflictInsteadOfOverwritingCredentials() {
        var service=registrationWithConstraint("23505");
        assertThrows(com.net.cardo.xcopybackend.exception.exceptions.EmailAlreadyExistsException.class,
                ()->service.verifyCode(registrationChallenge,"123456"));
    }

    @Test void unrelatedDatabaseIntegrityFailuresAreNotReportedAsDuplicateEmails() {
        var service=registrationWithConstraint("23502");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->service.verifyCode(registrationChallenge,"123456"));
    }

    private String registrationChallenge;
    private TwoFactorService registrationWithConstraint(String state) {
        registrationChallenge=EphemeralAuthStore.random();
        String key="security:challenge:"+EphemeralAuthStore.digest(registrationChallenge);
        String email=Base64.getUrlEncoder().withoutPadding().encodeToString("New@Example.TEST".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String value=UUID.randomUUID()+"|0|otp-hash|"+email+"|pending-password";
        when(store.get(key)).thenReturn(value);when(store.consume(key,value)).thenReturn(true);
        when(passwords.matches("123456","otp-hash")).thenReturn(true);
        var constraint=new org.hibernate.exception.ConstraintViolationException("constraint",new java.sql.SQLException("constraint",state),"users_email_key_unique");
        when(users.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("constraint",constraint));
        return new TwoFactorService(mock(EmailService.class),mock(TwoFactorCodeGenerator.class),users,store,passwords);
    }

}
