package com.net.cardo.xcopybackend.twofactor.dto.request;

import java.util.UUID;

public record SendCodeRequest(UUID userId, String email) {
}
