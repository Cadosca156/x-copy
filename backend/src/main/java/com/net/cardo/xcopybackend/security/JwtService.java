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
    private final com.net.cardo.xcopybackend.user.repository.UserRepository users;
    public JwtService(@Value("${app.jwt.secret}") String secret, com.net.cardo.xcopybackend.user.repository.UserRepository users) {
        this.users = users;
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(secret); }
        catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("APP_JWT_SECRET must be Base64", invalid); }
        if (bytes.length < 32) throw new IllegalArgumentException("APP_JWT_SECRET must contain at least 32 random bytes");
        signingKey = Keys.hmacShaKeyFor(bytes);
    }
    public String generateToken(String email) {
        var user = users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("Unknown account"));
        return generateToken(user);
    }
    public String generateToken(com.net.cardo.xcopybackend.user.entity.User user) {
        // Bind to the version that passed authentication, including concurrent resets.
        return Jwts.builder().issuer("x-copy").subject(user.getEmail())
                .claim("uid", user.getId().toString()).claim("ver", user.getTokenVersion()).issuedAt(new Date())
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
        var claims = Jwts.parser().verifyWith(signingKey).requireIssuer("x-copy").build().parseSignedClaims(token).getPayload();
        return users.findByEmail(claims.getSubject()).map(user ->
                user.getId().toString().equals(claims.get("uid", String.class))
                && claims.get("ver") instanceof Number version
                && user.getTokenVersion() == version.intValue()).orElse(false);
    }
}
