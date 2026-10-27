package com.net.cardo.xcopybackend.auth.controller;

import com.net.cardo.xcopybackend.auth.service.AccountPasswordService;
import com.net.cardo.xcopybackend.auth.service.AccountPasswordService.PasswordStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/users/me/password")
@RequiredArgsConstructor
public class AccountPasswordController {
    private final AccountPasswordService passwords;

    public record SetPasswordRequest(String password) {}

    @GetMapping
    public PasswordStatus getStatus(Authentication authentication) {
        return passwords.getStatus(email(authentication));
    }

    @PostMapping
    public PasswordStatus setPassword(@RequestBody SetPasswordRequest request, Authentication authentication) {
        return passwords.setInitialPassword(email(authentication), request.password());
    }

    private String email(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(UNAUTHORIZED, "Увійдіть в акаунт, щоб задати пароль.");
        }
        return authentication.getPrincipal() instanceof OidcUser oidc
                ? oidc.getEmail() : authentication.getName();
    }
}
