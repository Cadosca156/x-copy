package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAdversarialTest {
    private final byte[] key = new byte[64];
    private final UserRepository users = mock(UserRepository.class);
    private final JwtService jwt = new JwtService(Base64.getEncoder().encodeToString(key), users);
    private User user(int version) {
        var user = new User();
        user.setId(UUID.fromString("e5525401-0a3b-4f3f-b0bb-c14e9440ca3c"));
        user.setEmail("user@example.test"); user.setTokenVersion(version);
        return user;
    }
    @Test void expiredOrWrongIssuerTokensCannotAuthenticate() {
        var expired = Jwts.builder().issuer("x-copy").subject("user@example.test")
            .expiration(new Date(System.currentTimeMillis()-60_000))
            .signWith(Keys.hmacShaKeyFor(key), Jwts.SIG.HS256).compact();
        var wrongIssuer = Jwts.builder().issuer("another-app").subject("user@example.test")
            .signWith(Keys.hmacShaKeyFor(key), Jwts.SIG.HS256).compact();
        assertThrows(JwtException.class, () -> jwt.validateToken(expired));
        assertThrows(JwtException.class, () -> jwt.validateToken(wrongIssuer));
        verifyNoInteractions(users);
    }
    @Test void legacyTokensWithoutAccountBindingAndDeletedAccountsAreRejected() {
        when(users.findByEmail("user@example.test")).thenReturn(Optional.of(user(0)));
        var legacy = Jwts.builder().issuer("x-copy").subject("user@example.test")
            .signWith(Keys.hmacShaKeyFor(key), Jwts.SIG.HS256).compact();
        assertFalse(jwt.validateToken(legacy));
        String token = jwt.generateToken(user(0));
        when(users.findByEmail("user@example.test")).thenReturn(Optional.empty());
        assertFalse(jwt.validateToken(token));
    }
    @Test void concurrentPasswordResetCannotUpgradeAnOlderAuthenticationSnapshot() {
        when(users.findByEmail("user@example.test")).thenReturn(Optional.of(user(1)));
        // Authentication started before the reset and retained version zero.
        String stale = jwt.generateToken(user(0));
        assertFalse(jwt.validateToken(stale));
        assertTrue(jwt.validateToken(jwt.generateToken("user@example.test")));
    }
}
