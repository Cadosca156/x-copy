package com.net.cardo.xcopybackend.user.dto.request;

public record UpdateProfileRequest(
        String username,
        String displayName
) {}