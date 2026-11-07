package com.net.cardo.xcopybackend.security;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

/** Redis stores identity only: no password hash, Google access token or JPA entity. */
public record SessionPrincipal(UUID userId, String email) implements Principal, Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @Override public String getName() { return email; }
}
