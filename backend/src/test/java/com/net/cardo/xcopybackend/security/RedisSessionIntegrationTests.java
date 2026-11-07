package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.exception.exceptions.AuthenticationRequiredException;
import com.net.cardo.xcopybackend.passwordreset.dto.request.ResetPasswordRequest;
import com.net.cardo.xcopybackend.passwordreset.entity.PasswordResetToken;
import com.net.cardo.xcopybackend.passwordreset.repository.PasswordResetTokenRepository;
import com.net.cardo.xcopybackend.passwordreset.service.PasswordResetService;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import jakarta.servlet.http.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.*;
import org.springframework.session.web.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.options=-c statement_timeout=5000 -c lock_timeout=4000")
class RedisSessionIntegrationTests {
    @Autowired BrowserSessionService sessions;
    @Autowired FindByIndexNameSessionRepository<? extends Session> repository;
    @Autowired CookieSerializer serializer;
    @Autowired SecurityContextRepository contexts;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordResetService resets;
    @Autowired PasswordEncoder passwords;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    private final List<UUID> accounts = new ArrayList<>();
    private final List<PasswordResetToken> links = new ArrayList<>();

    @AfterEach void cleanup() {
        SecurityContextHolder.clearContext();
        accounts.forEach(sessions::revokeAll);
        tokens.deleteAll(links);
        users.deleteAllById(accounts);
    }

    private User account() {
        var user = new User();user.setEmail("session-" + UUID.randomUUID() + "@example.test");user.setPassword(passwords.encode("old-password"));
        user = users.saveAndFlush(user);accounts.add(user.getId());return user;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private SessionRepositoryFilter<?> filter() {
        var filter = new SessionRepositoryFilter(repository);
        var resolver = new CookieHttpSessionIdResolver();resolver.setCookieSerializer(serializer);
        filter.setHttpSessionIdResolver(resolver);return filter;
    }

    private Cookie login(User user) throws Exception {
        var response = new MockHttpServletResponse();
        filter().doFilter(new MockHttpServletRequest(), response, (req, res) ->
                sessions.authenticate(LoginProof.from(user), (HttpServletRequest) req, (HttpServletResponse) res));
        SecurityContextHolder.clearContext();
        String cookie = response.getHeaders("Set-Cookie").stream().filter(c -> c.startsWith(BrowserSessionService.COOKIE + "=")).findFirst().orElseThrow();
        assertTrue(cookie.contains("HttpOnly"));assertTrue(cookie.contains("Secure"));assertTrue(cookie.contains("SameSite=Lax"));assertTrue(cookie.contains("Path=/"));
        return new Cookie(BrowserSessionService.COOKIE, cookie.substring(cookie.indexOf('=') + 1, cookie.indexOf(';')));
    }

    private Authentication restore(Cookie cookie) throws Exception {
        var request = new MockHttpServletRequest();request.setCookies(cookie);
        var auth = new java.util.concurrent.atomic.AtomicReference<Authentication>();
        // A fresh filter represents another backend process using the same Redis session repository.
        filter().doFilter(request, new MockHttpServletResponse(), (req, res) ->
                auth.set(contexts.loadDeferredContext((HttpServletRequest) req).get().getAuthentication()));
        return auth.get();
    }

    private ResetPasswordRequest resetRequest(User user) {
        var token = new PasswordResetToken();token.setUserId(user.getId());token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(LocalDateTime.now().plusMinutes(10));token = tokens.saveAndFlush(token);links.add(token);
        var request = new ResetPasswordRequest();request.setToken(token.getToken());request.setNewPassword("new-password");return request;
    }

    @Test void realRedisPersistsAnOpaqueCookieAndRestoresIdentityAcrossRequests() throws Exception {
        var user = account();var cookie = login(user);
        assertDoesNotThrow(() -> UUID.fromString(cookie.getValue()));assertFalse(cookie.getValue().contains("."));
        assertEquals(new SessionPrincipal(user.getId(), user.getEmail()), restore(cookie).getPrincipal());
        assertTrue(repository.findByPrincipalName(user.getId().toString()).containsKey(cookie.getValue()));
        assertNotNull(repository.findById(cookie.getValue()));
    }

    @Test void logoutDeletesOnlyTheCurrentRedisSessionAndAnotherDeviceRemainsAuthenticated() throws Exception {
        var user = account();var first = login(user);var second = login(user);
        var request = new MockHttpServletRequest();request.setCookies(first);
        filter().doFilter(request, new MockHttpServletResponse(), (req, res) -> sessions.logout(null, (HttpServletRequest) req, (HttpServletResponse) res));
        assertNull(restore(first));assertNotNull(restore(second));
    }

    @Test void passwordResetRevokesBothDevicesAndRejectsPreviouslyVerifiedProofs() throws Exception {
        var user = account();var oldProof = LoginProof.from(user);var first = login(user);var second = login(user);
        resets.resetPassword(resetRequest(user));
        assertNull(restore(first));assertNull(restore(second));assertTrue(repository.findByPrincipalName(user.getId().toString()).isEmpty());
        assertEquals(1, users.findById(user.getId()).orElseThrow().getCredentialVersion());
        assertThrows(AuthenticationRequiredException.class, () -> sessions.authenticate(oldProof, new MockHttpServletRequest(), new MockHttpServletResponse()));
    }

    @Test void finishingAnInflightRequestCannotRestoreTheRevokedSecurityContext() throws Exception {
        var user = account();var reset = resetRequest(user);var response = new MockHttpServletResponse();
        var id = new java.util.concurrent.atomic.AtomicReference<String>();
        filter().doFilter(new MockHttpServletRequest(), response, (req, res) -> {
            sessions.authenticate(LoginProof.from(user), (HttpServletRequest) req, (HttpServletResponse) res);
            id.set(((HttpServletRequest) req).getSession(false).getId());
            // Login committed its index/context, but the HTTP request has not reached the session filter's final save yet.
            resets.resetPassword(reset);
        });
        SecurityContextHolder.clearContext();
        assertNull(restore(new Cookie(BrowserSessionService.COOKIE, id.get())));
    }

    @Test void anExpiredRedisSessionDoesNotRestoreAuthentication() throws Exception {
        var user = account();var cookie = login(user);
        expire(cookie.getValue());assertNull(restore(cookie));
    }


    @Test void sqlRollbackMayRevokeSessionsButDoesNotCommitTheNewPassword() throws Exception {
        var user = account();var cookie = login(user);var request = resetRequest(user);
        new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
            resets.resetPassword(request);
            status.setRollbackOnly();
        });
        var unchanged = users.findById(user.getId()).orElseThrow();
        assertTrue(passwords.matches("old-password", unchanged.getPassword()));assertEquals(0, unchanged.getCredentialVersion());
        Boolean used = new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status -> tokens.findByToken(request.getToken()).orElseThrow().isUsed());
        assertEquals(Boolean.FALSE, used);
        assertNull(restore(cookie));
    }

    @Test void loginWaitingForAConcurrentResetCannotIssueAStaleSession() throws Exception {
        var user = account();var proof = LoginProof.from(user);var request = resetRequest(user);
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        var started = new java.util.concurrent.CountDownLatch(1);
        var pending = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<?>>();
        try {
            new org.springframework.transaction.support.TransactionTemplate(transactions).executeWithoutResult(status -> {
                users.findForAuthentication(user.getId()).orElseThrow(); // Real PostgreSQL row lock.
                pending.set(executor.submit(() -> {
                    started.countDown();
                    try { sessions.authenticate(proof, new MockHttpServletRequest(), new MockHttpServletResponse()); }
                    finally { SecurityContextHolder.clearContext(); }
                }));
                try { assertTrue(started.await(2, java.util.concurrent.TimeUnit.SECONDS)); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt();throw new AssertionError(interrupted); }
                assertThrows(java.util.concurrent.TimeoutException.class, () -> pending.get().get(100, java.util.concurrent.TimeUnit.MILLISECONDS));
                resets.resetPassword(request);
            });
            var failure = assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> pending.get().get(5, java.util.concurrent.TimeUnit.SECONDS));
            assertInstanceOf(AuthenticationRequiredException.class, failure.getCause());
            assertTrue(repository.findByPrincipalName(user.getId().toString()).isEmpty());
        } finally { executor.shutdownNow(); }
    }

    private <S extends Session> void expireSession(FindByIndexNameSessionRepository<S> repo, String id) {
        S session = repo.findById(id);session.setMaxInactiveInterval(Duration.ofSeconds(1));
        session.setLastAccessedTime(Instant.now().minusSeconds(5));repo.save(session);
    }
    private void expire(String id) { expireSession(repository, id); }
}
