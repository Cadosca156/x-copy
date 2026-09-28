package com.net.cardo.xcopybackend.dto.request;

import java.util.UUID;

public record VerifyTwoFactorRequest(UUID userId,String code) {


}
