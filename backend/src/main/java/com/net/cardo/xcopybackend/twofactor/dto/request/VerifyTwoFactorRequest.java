package com.net.cardo.xcopybackend.twofactor.dto.request;

import java.util.UUID;

public record VerifyTwoFactorRequest(UUID userId,String code) {


}
