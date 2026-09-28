package com.net.cardo.xcopybackend.controller;


import com.net.cardo.xcopybackend.dto.request.VerifyTwoFactorRequest;
import com.net.cardo.xcopybackend.dto.response.TwoFactorResponse;
import com.net.cardo.xcopybackend.service.TwoFactorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")

public class TwoFactorCodeController {
    private final TwoFactorService twoFactorService;

    @PostMapping("/verify-2fa")
    public ResponseEntity<TwoFactorResponse> verify(
            @RequestBody VerifyTwoFactorRequest request
    ) {
    String token = twoFactorService.verifyCode(
            request.userId(),
            request.code()
    );
        return ResponseEntity.ok(new TwoFactorResponse(token));
    }
}
