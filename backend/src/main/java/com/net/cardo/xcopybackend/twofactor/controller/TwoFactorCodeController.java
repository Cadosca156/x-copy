package com.net.cardo.xcopybackend.twofactor.controller;


import com.net.cardo.xcopybackend.twofactor.dto.request.SendCodeRequest;
import com.net.cardo.xcopybackend.twofactor.dto.request.VerifyTwoFactorRequest;
import com.net.cardo.xcopybackend.twofactor.dto.response.TwoFactorResponse;
import com.net.cardo.xcopybackend.twofactor.service.TwoFactorService;
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
    private final com.net.cardo.xcopybackend.security.BrowserSessionService sessions;

    @PostMapping("/verify-2fa")
    public ResponseEntity<java.util.Map<String,Boolean>> verify(
            @RequestBody VerifyTwoFactorRequest request,
            jakarta.servlet.http.HttpServletResponse response
    ) {
    String token = twoFactorService.verifyCode(
            request.challengeId(),
            request.code()
    );
        sessions.authenticate(response,token);
        return ResponseEntity.ok(java.util.Map.of("authenticated",true));
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(
            @RequestBody SendCodeRequest request
    ) {twoFactorService.resend(request.challengeId());
        return  ResponseEntity.ok().build() ;

    }}
