package com.net.cardo.xcopybackend.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    public JwtService(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("APP_JWT_SECRET must be Base64", invalid); }
        if (bytes.length < 32) throw new IllegalArgumentException("APP_JWT_SECRET must contain at least 32 random bytes");
        signingKey = Keys.hmacShaKeyFor(bytes);
    }
    public String generateToken(String email) {
        return Jwts.builder().subject(email).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86_400_000L))
                .signWith(signingKey, Jwts.SIG.HS256).compact();
    }
    public String extractEmail(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload().getSubject();
    }
    public Date extractExpiration(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload().getExpiration();
    }
    public Boolean validateToken(String token) {
        return extractExpiration(token).after(new Date());
    }
}
