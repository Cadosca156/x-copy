package com.net.cardo.xcopybackend.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private UUID userId;

    private String challengeId;

}
