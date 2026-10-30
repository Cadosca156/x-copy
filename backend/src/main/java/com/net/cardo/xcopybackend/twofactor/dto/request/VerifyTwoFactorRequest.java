package com.net.cardo.xcopybackend.twofactor.dto.request;
public record VerifyTwoFactorRequest(String challengeId, String code) {}
