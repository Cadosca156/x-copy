package com.net.cardo.xcopybackend.security;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JwtKeyTest {
    private String key(byte value) {
        byte[] bytes = new byte[64];
        java.util.Arrays.fill(bytes, value);
        return Base64.getEncoder().encodeToString(bytes);
    }
    @Test void secretMustBeConfiguredAndStrongEnough() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("invalid base64"));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(Base64.getEncoder().encodeToString(new byte[8])));
    }
    @Test void rotatedKeyRejectsOldTokens() {
        var old = new JwtService(key((byte) 1));
        var current = new JwtService(key((byte) 2));
        String token = old.generateToken("audit@example.test");
        assertEquals("audit@example.test", old.extractEmail(token));
        assertThrows(io.jsonwebtoken.JwtException.class, () -> current.extractEmail(token));
    }
}
