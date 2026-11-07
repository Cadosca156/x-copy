package com.net.cardo.xcopybackend.security;

import com.net.cardo.xcopybackend.user.entity.User;
import java.util.UUID;

/** Internal proof produced by 2FA/OAuth, never accepted as a client authentication payload. */
public record LoginProof(UUID userId, int credentialVersion) {
    public static LoginProof from(User user) {
        return new LoginProof(user.getId(), user.getCredentialVersion());
    }
}
