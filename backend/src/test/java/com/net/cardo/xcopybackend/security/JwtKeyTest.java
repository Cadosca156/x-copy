package com.net.cardo.xcopybackend.security;
import java.util.*;
import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class JwtKeyTest {
    private String key(byte value) {
        byte[] bytes=new byte[64]; Arrays.fill(bytes,value);
        return Base64.getEncoder().encodeToString(bytes);
    }
    @Test void secretMustBeConfiguredAndStrongEnough() {
        var users=mock(UserRepository.class);
        assertThrows(IllegalArgumentException.class,()->new JwtService("invalid base64",users));
        assertThrows(IllegalArgumentException.class,()->new JwtService(Base64.getEncoder().encodeToString(new byte[8]),users));
    }
    @Test void rotationResetAndAccountRecreationInvalidateOldSessions() {
        var users=mock(UserRepository.class);
        var user=new User();user.setId(UUID.randomUUID());user.setEmail("audit@example.test");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        var jwt=new JwtService(key((byte)1),users);
        String token=jwt.generateToken(user.getEmail());
        assertTrue(jwt.validateToken(token));
        user.setTokenVersion(1);
        assertFalse(jwt.validateToken(token));
        String current=jwt.generateToken(user.getEmail());
        assertTrue(jwt.validateToken(current));
        user.setId(UUID.randomUUID()); // the same email now belongs to a different account
        assertFalse(jwt.validateToken(current));
        assertThrows(io.jsonwebtoken.JwtException.class,()->new JwtService(key((byte)2),users).extractEmail(current));
    }
}
