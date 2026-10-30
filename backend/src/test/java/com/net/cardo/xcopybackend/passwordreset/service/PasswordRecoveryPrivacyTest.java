package com.net.cardo.xcopybackend.passwordreset.service;
import com.net.cardo.xcopybackend.config.FrontendLinks;
import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.passwordreset.controller.PasswordResetController;
import com.net.cardo.xcopybackend.passwordreset.dto.request.ForgotPasswordRequest;
import com.net.cardo.xcopybackend.passwordreset.repository.PasswordResetTokenRepository;
import com.net.cardo.xcopybackend.security.*;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringJUnitConfig(PasswordRecoveryPrivacyTest.Config.class)
class PasswordRecoveryPrivacyTest {
    @Configuration @Import({SecurityMailConfig.class,PasswordResetService.class,PasswordResetController.class})
    static class Config {
        @Bean UserRepository users(){return mock(UserRepository.class);}
        @Bean PasswordResetTokenRepository tokens(){return mock(PasswordResetTokenRepository.class);}
        @Bean EmailService mail(){return mock(EmailService.class);}
        @Bean PasswordEncoder passwords(){return mock(PasswordEncoder.class);}
        @Bean EphemeralAuthStore store(){return mock(EphemeralAuthStore.class);}
        @Bean FrontendLinks links(){return new FrontendLinks("https://app.example.test");}
    }
    @Autowired PasswordResetController controller;
    @Autowired UserRepository users;
    @Autowired EmailService mail;
    @Test void knownAndUnknownEmailsHaveTheSameResponseBeforeMailCompletes() throws Exception {
        var user=new User();user.setId(UUID.randomUUID());user.setEmail("known@example.test");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(users.findByEmail("unknown@example.test")).thenReturn(Optional.empty());
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        doAnswer(call->{entered.countDown();release.await(5,TimeUnit.SECONDS);return null;})
            .when(mail).sendEmail(eq(user.getEmail()),anyString(),anyString());
        try {
            var known=controller.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
            var unknown=controller.forgotPassword(new ForgotPasswordRequest("unknown@example.test"));
            assertEquals(202,known.getStatusCode().value());assertEquals(known,unknown);
            assertTrue(entered.await(3,TimeUnit.SECONDS));assertNull(known.getBody());
            verify(users,timeout(3000)).findByEmail("unknown@example.test");
            verify(mail,never()).sendEmail(eq("unknown@example.test"),anyString(),anyString());
        } finally {release.countDown();}
    }
}
