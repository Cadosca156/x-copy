package com.net.cardo.xcopybackend.user.dto.response;

import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String username,
        String displayName,
        String avatarUrl
) {}