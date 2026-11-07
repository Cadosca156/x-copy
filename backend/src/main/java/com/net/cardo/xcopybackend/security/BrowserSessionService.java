package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.exception.exceptions.AuthenticationRequiredException;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import jakarta.servlet.http.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
public class BrowserSessionService {
    public static final String COOKIE = "XCOPY_SESSION_ID", BINDING = "XCOPY_OAUTH_BIND";
    private final EphemeralAuthStore store;
    private final UserRepository users;
    private final SecurityContextRepository contexts;
    private final FindByIndexNameSessionRepository<? extends Session> repository;
    @Value("${app.auth.secure-cookies:true}") private boolean secure = true;

    private void cookie(HttpServletResponse response, String name, String value, String path, Duration age) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(name, value).httpOnly(true).secure(secure)
                .sameSite("Lax").path(path).maxAge(age).build().toString());
        response.setHeader("Cache-Control", "no-store");
    }

    @Transactional
    public void authenticate(LoginProof proof, HttpServletRequest request, HttpServletResponse response) {
        // Serialize final session issuance with password reset, across all backend instances.
        var user = users.findForAuthentication(proof.userId()).orElseThrow(AuthenticationRequiredException::new);
        if (user.getCredentialVersion() != proof.credentialVersion()) throw new AuthenticationRequiredException();

        var previous = request.getSession(false);
        if (previous != null) previous.invalidate(); // Never promote a caller-chosen/pre-authentication session ID.
        var session = request.getSession(true);
        session.setMaxInactiveInterval(1800);
        session.setAttribute(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, user.getId().toString());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(new SessionPrincipal(user.getId(), user.getEmail()),
                null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        // Redis IMMEDIATE flush writes the index and context while the account row lock is still held.
        contexts.saveContext(context, request, response);
        SecurityContextHolder.setContext(context);
        cookie(response, "XCOPY_SESSION", "", "/api", Duration.ZERO); // Expire the retired JWT cookie.
    }

    public String beginOAuth(HttpServletResponse response) {
        String binding = EphemeralAuthStore.random(), start = EphemeralAuthStore.random();
        cookie(response, BINDING, binding, "/api", Duration.ofMinutes(5));
        store.put("security:oauth-start:" + EphemeralAuthStore.digest(start), binding, Duration.ofMinutes(5));
        return start;
    }

    public String issueOAuthCode(String email, String binding) {
        if (binding == null || binding.isBlank()) throw invalid();
        var user = users.findByEmail(email).orElseThrow(AuthenticationRequiredException::new);
        String code = EphemeralAuthStore.random();
        store.put("security:oauth-code:" + EphemeralAuthStore.digest(code),
                binding + "|" + user.getId() + "|" + user.getCredentialVersion(), Duration.ofSeconds(60));
        return code;
    }

    @Transactional
    public void exchange(String code, HttpServletRequest request, HttpServletResponse response) {
        if (code == null || !code.matches("[A-Za-z0-9_-]{43}")) throw invalid();
        String binding = Arrays.stream(request.getCookies() == null ? new Cookie[0] : request.getCookies())
                .filter(c -> BINDING.equals(c.getName())).map(Cookie::getValue).findFirst().orElse("");
        String value = store.take("security:oauth-code:" + EphemeralAuthStore.digest(code));
        if (value == null || binding.isBlank()) throw invalid();
        String[] fields = value.split("\\|", -1);
        if (fields.length != 3 || !fields[0].equals(binding)) throw invalid();
        LoginProof proof;
        try { proof = new LoginProof(UUID.fromString(fields[1]), Integer.parseInt(fields[2])); }
        catch (IllegalArgumentException malformed) { throw invalid(); }
        authenticate(proof, request, response);
        cookie(response, BINDING, "", "/api", Duration.ZERO);
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Сесію Google-входу завершено. Спробуйте знову.");
    }

    /** Called while password reset holds the same account row lock as authenticate(). */
    public void revokeAll(UUID userId) {
        repository.findByPrincipalName(userId.toString()).keySet().forEach(repository::deleteById);
    }

    public void logout(Authentication auth, HttpServletRequest request, HttpServletResponse response) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        // Explicitly remove cookies even if the session had already disappeared from Redis.
        cookie(response, COOKIE, "", "/", Duration.ZERO);
        cookie(response, "XCOPY_SESSION", "", "/api", Duration.ZERO);
    }
}
