package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.exception.exceptions.AuthenticationRequiredException;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BrowserSessionTest {
    private final EphemeralAuthStore store = mock(EphemeralAuthStore.class);
    private final UserRepository users = mock(UserRepository.class);
    @SuppressWarnings("unchecked")
    private final FindByIndexNameSessionRepository<Session> repository = mock(FindByIndexNameSessionRepository.class);
    private final BrowserSessionService sessions = new BrowserSessionService(store, users,
            new HttpSessionSecurityContextRepository(), repository);

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private User user(int version) {
        var user = new User();user.setId(UUID.randomUUID());user.setEmail("me@example.test");user.setCredentialVersion(version);
        when(users.findForAuthentication(user.getId())).thenReturn(Optional.of(user));
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return user;
    }

    @Test void authenticationReplacesPreLoginSessionAndStoresOnlyIdentity() throws Exception {
        var user = user(0);var request = new MockHttpServletRequest();var old = request.getSession();String previousId = old.getId();
        old.setAttribute("untrusted", "value");var response = new MockHttpServletResponse();
        sessions.authenticate(LoginProof.from(user), request, response);
        var fresh = request.getSession(false);assertNotEquals(previousId, fresh.getId());assertNull(fresh.getAttribute("untrusted"));
        assertEquals(1800, fresh.getMaxInactiveInterval());
        assertEquals(user.getId().toString(), fresh.getAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME));
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(new SessionPrincipal(user.getId(), user.getEmail()), auth.getPrincipal());assertNull(auth.getCredentials());
        assertSame(SecurityContextHolder.getContext(), fresh.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertEquals("", response.getContentAsString());assertEquals("no-store", response.getHeader("Cache-Control"));
    }

    @Test void proofFromBeforePasswordResetCannotIssueANewSession() {
        var user = user(1);var request = new MockHttpServletRequest();
        assertThrows(AuthenticationRequiredException.class, () -> sessions.authenticate(new LoginProof(user.getId(), 0), request, new MockHttpServletResponse()));
        assertNull(request.getSession(false));
    }

    @Test void deletedAccountCannotIssueASession() {
        var request = new MockHttpServletRequest();
        assertThrows(AuthenticationRequiredException.class, () -> sessions.authenticate(new LoginProof(UUID.randomUUID(), 0), request, new MockHttpServletResponse()));
        assertNull(request.getSession(false));
    }

    @Test void oauthCodeContainsOnlyBindingIdentityAndCredentialVersion() {
        var user = user(2);String code = sessions.issueOAuthCode(user.getEmail(), "browser");
        verify(store).put("security:oauth-code:" + EphemeralAuthStore.digest(code), "browser|" + user.getId() + "|2", java.time.Duration.ofSeconds(60));
        assertThrows(ResponseStatusException.class, () -> sessions.issueOAuthCode(user.getEmail(), null));
    }

    @Test void exchangeIsBoundToTheBrowserAndSingleUse() {
        var user = user(0);String code = "a".repeat(43);String key = "security:oauth-code:" + EphemeralAuthStore.digest(code);
        var request = new MockHttpServletRequest();request.setCookies(new Cookie(BrowserSessionService.BINDING, "right"));
        when(store.take(key)).thenReturn("right|" + user.getId() + "|0").thenReturn(null);
        sessions.exchange(code, request, new MockHttpServletResponse());assertNotNull(request.getSession(false));
        assertThrows(ResponseStatusException.class, () -> sessions.exchange(code, request, new MockHttpServletResponse()));
    }

    @Test void wrongBrowserMissingBindingMalformedProofAndResetProofCannotAuthenticate() {
        String code = "b".repeat(43);String key = "security:oauth-code:" + EphemeralAuthStore.digest(code);
        for (String stored : new String[]{"other|id|0", "mine|invalid|0", "mine|id|invalid", "mine|id"}) {
            var request = new MockHttpServletRequest();request.setCookies(new Cookie(BrowserSessionService.BINDING, "mine"));
            when(store.take(key)).thenReturn(stored);
            assertThrows(ResponseStatusException.class, () -> sessions.exchange(code, request, new MockHttpServletResponse()));
            assertNull(request.getSession(false));
        }
        var user = user(1);var request = new MockHttpServletRequest();request.setCookies(new Cookie(BrowserSessionService.BINDING, "mine"));
        when(store.take(key)).thenReturn("mine|" + user.getId() + "|0");
        assertThrows(AuthenticationRequiredException.class, () -> sessions.exchange(code, request, new MockHttpServletResponse()));
        var withoutBinding = new MockHttpServletRequest();
        assertThrows(ResponseStatusException.class, () -> sessions.exchange(code, withoutBinding, new MockHttpServletResponse()));
        assertNull(withoutBinding.getSession(false));
    }

    @Test void logoutInvalidatesOnlyTheCurrentSessionWithoutDatabaseOrIndexQueries() {
        var request = new MockHttpServletRequest();var old = (MockHttpSession) request.getSession();var response = new MockHttpServletResponse();
        sessions.logout(null, request, response);
        assertTrue(old.isInvalid());assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(users, repository);
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(c -> c.startsWith(BrowserSessionService.COOKIE + "=") && c.contains("Max-Age=0")));
    }

    @Test void revokeAllUsesTheAccountIndexAndDeletesEveryMatchingSession() {
        var id = UUID.randomUUID();when(repository.findByPrincipalName(id.toString())).thenReturn(Map.of("one", mock(Session.class), "two", mock(Session.class)));
        sessions.revokeAll(id);verify(repository).deleteById("one");verify(repository).deleteById("two");
    }
}
