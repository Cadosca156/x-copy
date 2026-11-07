package com.net.cardo.xcopybackend.security;

import java.time.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

class SessionFiltersTest {
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test void absoluteExpiryClearsAuthenticationButAllowsTheChainToApplyPublicOrProtectedRules() throws Exception {
        var request = new MockHttpServletRequest();var session = (MockHttpSession) request.getSession();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new SessionPrincipal(UUID.randomUUID(), "user@example.test"), null, List.of()));
        var clock = Clock.fixed(Instant.ofEpochMilli(session.getCreationTime()).plus(Duration.ofDays(1)), ZoneOffset.UTC);
        new SessionLifetimeFilter(clock).doFilter(request, new MockHttpServletResponse(), (req, res) ->
                assertNull(SecurityContextHolder.getContext().getAuthentication()));
        assertTrue(session.isInvalid());
    }

    @Test void validSessionKeepsItsAuthentication() throws Exception {
        var request = new MockHttpServletRequest();request.getSession();
        var auth = new UsernamePasswordAuthenticationToken(new SessionPrincipal(UUID.randomUUID(), "user@example.test"), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        new SessionLifetimeFilter(Clock.systemUTC()).doFilter(request, new MockHttpServletResponse(), (req, res) ->
                assertSame(auth, SecurityContextHolder.getContext().getAuthentication()));
    }

    @Test void redisFailureOutsideMvcReturnsRetryableJsonWithoutInternalDetails() throws Exception {
        var response = new MockHttpServletResponse();
        new SessionAvailabilityFilter().doFilter(new MockHttpServletRequest(), response, (req, res) -> {
            throw new RedisConnectionFailureException("private-redis-address");
        });
        assertEquals(503, response.getStatus());assertEquals("2", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("\"status\":503"));assertFalse(response.getContentAsString().contains("private-redis"));
    }

    @Test void programmingErrorsAreNotMisclassifiedAsRedisOutages() {
        assertThrows(IllegalArgumentException.class, () -> new SessionAvailabilityFilter().doFilter(
                new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> { throw new IllegalArgumentException("bug"); }));
    }
}
