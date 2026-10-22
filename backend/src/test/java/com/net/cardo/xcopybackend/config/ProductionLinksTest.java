package com.net.cardo.xcopybackend.config;

import com.net.cardo.xcopybackend.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.net.cardo.xcopybackend.auth.service.CustomUserDetailsService;
import com.net.cardo.xcopybackend.email.EmailService;
import com.net.cardo.xcopybackend.passwordreset.dto.request.ForgotPasswordRequest;
import com.net.cardo.xcopybackend.passwordreset.repository.PasswordResetTokenRepository;
import com.net.cardo.xcopybackend.passwordreset.service.PasswordResetService;
import com.net.cardo.xcopybackend.security.JwtService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductionLinksTest {
    private static final String FRONTEND = "https://main.d2s4oyee9a0d3l.amplifyapp.com";

    @Test
    void buildsProductionLinksAndNormalizesTrailingSlash() {
        var links = new FrontendLinks(FRONTEND + "/");
        assertEquals(FRONTEND + "/oauth2/success?token=jwt", links.oauthSuccess("jwt"));
        assertEquals(FRONTEND + "/reset-password?token=reset", links.resetPassword("reset"));
        assertEquals(FRONTEND + "/reset-password?token=a%26b", links.resetPassword("a&b"));
    }

    @Test
    void supportsLocalDevelopment() {
        assertEquals("http://localhost:5173/reset-password?token=test",
                new FrontendLinks("http://localhost:5173").resetPassword("test"));
    }

    @Test
    void rejectsMalformedFrontendConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new FrontendLinks("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> new FrontendLinks(FRONTEND + "?redirect=other"));
    }

    @Test
    void redirectsGoogleLoginToAmplify() throws Exception {
        var jwt = mock(JwtService.class);
        when(jwt.generateToken("user@example.test")).thenReturn("test-jwt");
        var handler = new OAuth2AuthenticationSuccessHandler(jwt, new FrontendLinks(FRONTEND));
        var principal = mock(OAuth2User.class);
        when(principal.getAttribute("email")).thenReturn("user@example.test");
        var authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        var response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);
        assertEquals(FRONTEND + "/oauth2/success?token=test-jwt", response.getRedirectedUrl());
    }

    @Test
    void createsResetEmailWithProductionLink() {
        var users = mock(UserRepository.class);
        var tokens = mock(PasswordResetTokenRepository.class);
        var mail = mock(EmailService.class);
        var user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.test");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var service = new PasswordResetService(tokens, users, mock(org.springframework.security.crypto.password.PasswordEncoder.class), mail, new FrontendLinks(FRONTEND));
        service.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
        var message = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(mail).sendEmail(eq(user.getEmail()), eq("Reset password"), message.capture());
        assertTrue(message.getValue().contains(FRONTEND + "/reset-password?token="));
        assertFalse(message.getValue().contains("localhost"));
        verify(tokens).save(any());
    }

    @Test
    void loadsGoogleUserWithoutPasswordForJwtAuthentication() {
        var users = mock(UserRepository.class);
        var user = new User();
        user.setEmail("google@example.test");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var details = new CustomUserDetailsService(users).loadUserByUsername(user.getEmail());
        assertEquals(user.getEmail(), details.getUsername());
        assertNotNull(details.getPassword());
    }
}
