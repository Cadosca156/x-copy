package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.user.entity.User;
import com.net.cardo.xcopybackend.user.repository.UserRepository;
import java.util.Optional;
import org.springframework.security.core.context.SecurityContextHolder;

/** Resource-level lookup: an existing session must never attach to a new account reusing an email. */
public final class SessionAccounts {
    private SessionAccounts() {}
    public static Optional<User> find(UserRepository users, String email) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof SessionPrincipal principal) {
            if (!com.net.cardo.xcopybackend.auth.EmailAddress.same(principal.email(), email)) return Optional.empty();
            return users.findById(principal.userId()).filter(user -> com.net.cardo.xcopybackend.auth.EmailAddress.same(user.getEmail(), email));
        }
        return users.findByEmail(email);
    }
}
