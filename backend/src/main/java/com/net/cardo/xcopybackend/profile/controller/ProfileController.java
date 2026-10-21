package com.net.cardo.xcopybackend.profile.controller;


import com.net.cardo.xcopybackend.profile.service.ProfileService;
import com.net.cardo.xcopybackend.user.dto.request.UpdateProfileRequest;
import com.net.cardo.xcopybackend.user.dto.response.ProfileResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PatchMapping("/me/profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        String email;

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
        } else {
            email = authentication.getName();
        }

        return ResponseEntity.ok(
                profileService.updateProfile(email, request)
        );
    }
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> updateAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        String email = authentication.getPrincipal() instanceof OidcUser oidcUser
                ? oidcUser.getEmail()
                : authentication.getName();
        return ResponseEntity.ok(profileService.updateAvatar(email, file));
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(
            Authentication authentication
    ) {
        String email;

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
        } else {
            email = authentication.getName();
        }

        return ResponseEntity.ok(
                profileService.getMyProfile(email)
        );
    }
}